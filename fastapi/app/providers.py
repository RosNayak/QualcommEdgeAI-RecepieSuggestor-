import os
import httpx
from typing import List, Dict

GEMINI_API_KEY = os.getenv("GEMINI_API_KEY", "")
GEMINI_MODEL = os.getenv("GEMINI_MODEL", "gemini-1.5-flash")

GEMINI_URL = f"https://generativelanguage.googleapis.com/v1beta/models/{GEMINI_MODEL}:generateContent?key={GEMINI_API_KEY}"

PROMPT_TEMPLATE = """You are a helpful cooking assistant.
Given these ingredients: {ingredients}
servings: {servings}
dietary constraints (if any): {dietary}

Return 3 concise recipes. For each recipe include:
- Title
- Ingredients (subset of provided + minimal pantry items)
- 5-8 clear steps

Keep it short and practical.
"""

def build_prompt(ingredients: List[str], servings: int, dietary: List[str] | None) -> str:
    return PROMPT_TEMPLATE.format(
        ingredients=", ".join(ingredients) if ingredients else "none specified",
        servings=servings,
        dietary=", ".join(dietary) if dietary else "none"
    )

async def recipes_from_gemini(ingredients: List[str], servings: int, dietary: List[str] | None) -> Dict:
    if not GEMINI_API_KEY:
        raise RuntimeError("GEMINI_API_KEY not set on server")

    prompt = build_prompt(ingredients, servings, dietary)
    payload = {
        "contents": [
            {
                "parts": [
                    {"text": prompt}
                ]
            }
        ]
    }

    async with httpx.AsyncClient(timeout=30) as client:
        r = await client.post(GEMINI_URL, json=payload)
        r.raise_for_status()
        data = r.json()

    # Extract text safely
    text = ""
    try:
        text = data["candidates"][0]["content"]["parts"][0]["text"]
    except Exception:
        text = "Could not parse model response."

    # Very light parser: expect the model to produce blocks. Fallback to 1 recipe.
    blocks = [b.strip() for b in text.split("\n\n") if b.strip()]
    recipes = []
    current = {"title": "", "ingredients": [], "steps": []}

    def flush_current():
        if current["title"]:
            recipes.append({
                "title": current["title"],
                "ingredients": current["ingredients"] or [],
                "steps": current["steps"] or []
            })

    for line in blocks:
        low = line.lower()
        if "title:" in low:
            # start new recipe
            flush_current()
            current = {"title": line.split(":",1)[1].strip(), "ingredients": [], "steps": []}
        elif low.startswith("ingredients"):
            # split lines after colon
            items = [x.strip("-• ").strip() for x in line.split("\n")[1:]]
            current["ingredients"].extend([i for i in items if i])
        elif low.startswith("steps"):
            steps = [x.strip("-• ").strip() for x in line.split("\n")[1:]]
            current["steps"].extend([s for s in steps if s])
        else:
            # heuristic: if looks like a step bullet
            if line.startswith(("-", "•")):
                current["steps"].append(line.strip("-• ").strip())
            elif not current["title"]:
                current["title"] = line.strip()

    flush_current()
    if not recipes:
        recipes = [{
            "title": "Quick Pantry Stir-Fry",
            "ingredients": ingredients or ["mixed veggies", "rice", "soy sauce"],
            "steps": [
                "Heat a pan on medium-high.",
                "Add a little oil and sauté veggies 3–4 min.",
                "Stir in cooked rice and soy sauce; toss 2–3 min.",
                "Adjust seasoning and serve hot."
            ],
        }]

    return {"recipes": recipes, "model": GEMINI_MODEL, "provider": "gemini"}
