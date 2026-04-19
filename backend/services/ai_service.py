import anthropic
import base64
import json
import re
from app.core.config import settings

client = anthropic.Anthropic(api_key=settings.ANTHROPIC_API_KEY)

SYSTEM_PROMPT = "You are a professional appraiser with expertise in antiques, collectibles, electronics, jewelry, and watches. You identify items precisely and research actual current sold prices to give accurate market valuations."

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
        "text": f"""Analyze the provided images and description, then estimate the market value.
User description: {description if description else 'No description provided'}
CRITICAL: If the user provided a description, it is the PRIMARY source for item identification — trust it over your visual interpretation. Use the images only to assess condition, brand markings, and specific details.
IMPORTANT: Return ALL prices in {currency} currency. Search for recent sold prices on eBay and marketplaces to base your estimate on real market data. If web search returns no results, use your training knowledge to estimate based on similar items — never return 0 values.
Respond ONLY with a JSON object, no markdown, no explanation:
{{
  "item_name": "identified item name",
  "category": "category (watch/jewelry/electronics/collectible/furniture/art/other)",
  "condition": "excellent/good/fair/poor",
  "condition_notes": "brief condition assessment",
  "price_min": 0,
  "price_max": 0,
  "price_recommended": 0,
  "currency": "{currency}",
  "summary": "2-3 sentence summary in the same language as the description",
  "market_references_count": 0,
  "confidence": "high/medium/low"
}}"""
    })

    message = client.messages.create(
        model="claude-sonnet-4-5",
        max_tokens=2048,
        system=SYSTEM_PROMPT,
        tools=[{
            "type": "web_search_20250305",
            "name": "web_search",
            "max_uses": 2
        }],
        messages=[{"role": "user", "content": content}]
    )

    input_tokens  = message.usage.input_tokens
    output_tokens = message.usage.output_tokens

    for block in message.content:
        if block.type == "text":
            text = block.text.strip()
            try:
                result = json.loads(text)
                result["_tokens"] = {"input": input_tokens, "output": output_tokens}
                return result
            except json.JSONDecodeError:
                match = re.search(r'\{.*\}', text, re.DOTALL)
                if match:
                    result = json.loads(match.group())
                    result["_tokens"] = {"input": input_tokens, "output": output_tokens}
                    return result

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
