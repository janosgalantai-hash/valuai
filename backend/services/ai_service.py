import anthropic
import json
import re
from app.core.config import settings

client = anthropic.Anthropic(api_key=settings.ANTHROPIC_API_KEY)

SYSTEM_PROMPT = """You are a professional appraiser with 20+ years of experience in antiques, collectibles, electronics, jewelry, watches, and secondhand goods.
You have deep knowledge of eBay sold listings, auction house results, collector markets, and current retail pricing worldwide.
You always research actual recent sold prices before estimating value — never guess without market data.
You are precise, data-driven, and identify items as specifically as possible (brand, model, year, edition, serial range if visible)."""

def analyze_images_and_estimate(images_base64: list[str], description: str, currency: str = "USD") -> dict:
    content = []
    for img_b64 in images_base64:
        content.append({
            "type": "image",
            "source": {
                "type": "base64",
                "media_type": "image/jpeg",
                "data": img_b64
            }
        })

    content.append({
        "type": "text",
        "text": f"""Analyze the item in the provided image(s) and estimate its current market value.

User description: {description if description else "No description provided"}
Target currency: {currency}

Follow these steps before responding:
1. IDENTIFY the item as precisely as possible — brand, model, year, variant, edition, any visible markings
2. SEARCH eBay completed/sold listings for this exact item to find real transaction prices
3. SEARCH relevant auction house results, collector forums, or marketplace sold prices
4. SEARCH current retail price (new) if the item is still manufactured, as a reference point
5. ASSESS condition objectively from the images — note any visible wear, damage, or notable features
6. CALCULATE a realistic price range based on actual market data, weighted toward recent sold prices

After completing your research, respond ONLY with the following JSON object.
No markdown formatting, no code blocks, no explanation — raw JSON only:
{{
  "item_name": "precise item name including brand and model",
  "category": "watch/jewelry/electronics/collectible/furniture/art/clothing/other",
  "condition": "excellent/good/fair/poor",
  "condition_notes": "specific observations about condition based on the images",
  "price_min": <lowest realistic sold price as a number in {currency}>,
  "price_max": <highest realistic sold price as a number in {currency}>,
  "price_recommended": <most likely private sale price as a number in {currency}>,
  "currency": "{currency}",
  "summary": "2-3 sentences describing the item and its market value. Write this in the same language as the user description.",
  "market_references_count": <integer: number of market data points found>,
  "confidence": "high/medium/low"
}}"""
    })

    message = client.messages.create(
        model="claude-opus-4-5",
        max_tokens=4096,
        system=SYSTEM_PROMPT,
        tools=[{
            "type": "web_search_20250305",
            "name": "web_search"
        }],
        messages=[{"role": "user", "content": content}]
    )

    input_tokens  = message.usage.input_tokens
    output_tokens = message.usage.output_tokens

    for block in message.content:
        if block.type == "text":
            text = block.text.strip()
            text = re.sub(r"^```[a-z]*\n?", "", text)
            text = re.sub(r"\n?```$", "", text)
            text = text.strip()
            try:
                result = json.loads(text)
                result["_tokens"] = {"input": input_tokens, "output": output_tokens}
                return result
            except json.JSONDecodeError:
                match = re.search(r"\{.*\}", text, re.DOTALL)
                if match:
                    try:
                        result = json.loads(match.group())
                        result["_tokens"] = {"input": input_tokens, "output": output_tokens}
                        return result
                    except json.JSONDecodeError:
                        pass

    return {
        "item_name": "Unknown",
        "category": "other",
        "condition": "fair",
        "condition_notes": "Could not analyze",
        "price_min": 0,
        "price_max": 0,
        "price_recommended": 0,
        "currency": currency,
        "summary": "Analysis failed",
        "market_references_count": 0,
        "confidence": "low",
        "_tokens": {"input": input_tokens, "output": output_tokens}
    }
