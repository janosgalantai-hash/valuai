from fastapi import APIRouter, Depends, HTTPException, Request
from fastapi.responses import HTMLResponse
from sqlalchemy.orm import Session
from sqlalchemy import func
from app.core.database import get_db
from app.models.estimation import Estimation

router = APIRouter()

DASHBOARD_PASSWORD = "valuai_admin_2024"

DASHBOARD_HTML = """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>ValuAI Dashboard</title>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body { background: #0D0D14; color: #fff; font-family: -apple-system, sans-serif; padding: 24px; }
        h1 { color: #C8A96A; font-size: 28px; margin-bottom: 8px; }
        .subtitle { color: #808090; margin-bottom: 32px; }
        .grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 16px; margin-bottom: 32px; }
        .card { background: #141420; border-radius: 12px; padding: 20px; border: 1px solid #2A2A3A; }
        .card-label { color: #808090; font-size: 12px; letter-spacing: 1px; text-transform: uppercase; margin-bottom: 8px; }
        .card-value { font-size: 28px; font-weight: bold; color: #C8A96A; }
        .card-sub { color: #B0B0C0; font-size: 13px; margin-top: 4px; }
        table { width: 100%; border-collapse: collapse; background: #141420; border-radius: 12px; overflow: hidden; }
        th { background: #1A1A25; color: #808090; font-size: 11px; letter-spacing: 1px; text-transform: uppercase; padding: 12px 16px; text-align: left; }
        td { padding: 12px 16px; border-bottom: 1px solid #2A2A3A; color: #B0B0C0; font-size: 14px; }
        tr:last-child td { border-bottom: none; }
        .status-done { color: #4CAF50; }
        .status-failed { color: #E53935; }
        h2 { color: #fff; font-size: 18px; margin-bottom: 16px; }
        .refresh { color: #C8A96A; font-size: 13px; cursor: pointer; text-decoration: underline; }
    </style>
</head>
<body>
    <h1>ValuAI Dashboard</h1>
    <p class="subtitle">Token usage & cost overview — <span class="refresh" onclick="location.reload()">Refresh</span></p>

    <div class="grid">
        <div class="card">
            <div class="card-label">Total Estimations</div>
            <div class="card-value">{{total_estimations}}</div>
            <div class="card-sub">{{done}} done · {{failed}} failed</div>
        </div>
        <div class="card">
            <div class="card-label">Input Tokens</div>
            <div class="card-value">{{input_tokens}}</div>
            <div class="card-sub">${{input_cost}} USD</div>
        </div>
        <div class="card">
            <div class="card-label">Output Tokens</div>
            <div class="card-value">{{output_tokens}}</div>
            <div class="card-sub">${{output_cost}} USD</div>
        </div>
        <div class="card">
            <div class="card-label">Total Cost</div>
            <div class="card-value">${{total_cost}}</div>
            <div class="card-sub">Claude Sonnet 4.5</div>
        </div>
    </div>

    <h2>Recent Estimations</h2>
    <table>
        <thead>
            <tr>
                <th>Date</th>
                <th>Item</th>
                <th>Status</th>
                <th>Input Tokens</th>
                <th>Output Tokens</th>
                <th>Cost</th>
            </tr>
        </thead>
        <tbody>
            {{rows}}
        </tbody>
    </table>
</body>
</html>
"""

@router.get("/dashboard", response_class=HTMLResponse)
def dashboard(password: str = "", db: Session = Depends(get_db)):
    if password != DASHBOARD_PASSWORD:
        return HTMLResponse("""
            <html><body style="background:#0D0D14;color:#fff;font-family:sans-serif;display:flex;align-items:center;justify-content:center;height:100vh;flex-direction:column;gap:16px;">
            <h2 style="color:#C8A96A">ValuAI Dashboard</h2>
            <form method="get">
                <input name="password" type="password" placeholder="Password"
                    style="padding:10px;border-radius:8px;border:1px solid #C8A96A;background:#141420;color:#fff;font-size:16px;">
                <button type="submit"
                    style="padding:10px 20px;background:#C8A96A;border:none;border-radius:8px;cursor:pointer;font-weight:bold;margin-left:8px;">
                    Enter
                </button>
            </form>
            </body></html>
        """)

    estimations = db.query(Estimation).order_by(Estimation.created_at.desc()).limit(50).all()

    total = len(db.query(Estimation).all())
    done = len(db.query(Estimation).filter(Estimation.status == "done").all())
    failed = total - done

    total_input  = db.query(func.sum(Estimation.input_tokens)).scalar() or 0
    total_output = db.query(func.sum(Estimation.output_tokens)).scalar() or 0

    # Claude Sonnet 4.5 pricing
    input_cost  = round(total_input  / 1_000_000 * 3, 4)
    output_cost = round(total_output / 1_000_000 * 15, 4)
    total_cost  = round(input_cost + output_cost, 4)

    rows = ""
    for e in estimations:
        item_name = "—"
        if e.result and isinstance(e.result, dict) and "item_name" in e.result:
            item_name = e.result["item_name"][:40]
        row_cost = round((e.input_tokens / 1_000_000 * 3) + (e.output_tokens / 1_000_000 * 15), 4)
        status_class = "status-done" if e.status == "done" else "status-failed"
        date_str = e.created_at.strftime("%m/%d %H:%M") if e.created_at else "—"
        rows += f"""
            <tr>
                <td>{date_str}</td>
                <td>{item_name}</td>
                <td class="{status_class}">{e.status}</td>
                <td>{e.input_tokens or 0:,}</td>
                <td>{e.output_tokens or 0:,}</td>
                <td>${row_cost}</td>
            </tr>
        """

    html = DASHBOARD_HTML \
        .replace("{{total_estimations}}", str(total)) \
        .replace("{{done}}", str(done)) \
        .replace("{{failed}}", str(failed)) \
        .replace("{{input_tokens}}", f"{total_input:,}") \
        .replace("{{output_tokens}}", f"{total_output:,}") \
        .replace("{{input_cost}}", str(input_cost)) \
        .replace("{{output_cost}}", str(output_cost)) \
        .replace("{{total_cost}}", str(total_cost)) \
        .replace("{{rows}}", rows)

    return HTMLResponse(html)
