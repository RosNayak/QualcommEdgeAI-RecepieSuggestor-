import os
import httpx
from typing import List, Dict
from fastapi import HTTPException

GEMINI_API_KEY = os.getenv("GEMINI_API_KEY", "")
GEMINI_MODEL = os.getenv("GEMINI_MODEL", "gemini-1.5-flash")

GEMINI_URL = f"https://generativelanguage.googleapis.com/v1beta/models/{GEMINI_MODEL}:generateContent?key={GEMINI_API_KEY}"

PROMPT_TEMPLATE = """You are a helpful cooking assistant.
Given these ingredients: {ingredients}.
Consider only the food ingredients from this list.

Generate 3-5 realistic recipes that can be made with these ingredients. 
Return ONLY a JSON array with this exact format:
    title: Recipe Name
    description: Brief description
    ingredients: [ingredient1,ingredient2,ingredient3]
    instructions: Step 1 \n Step 2 \n Step 3

Make recipes practical and realistic. Use common cooking techniques.
"""

def build_prompt(ingredients: List[str], servings: int, dietary: List[str] | None) -> str:
    return PROMPT_TEMPLATE.format(
        ingredients=", ".join(ingredients) if ingredients else "none specified",
    )

async def recipes_from_gemini(ingredients: List[str], servings: int, dietary: List[str] | None) -> Dict:
    if not GEMINI_API_KEY:
        # Surface a clear error to the client instead of a generic 500
        raise HTTPException(status_code=503, detail="Server is missing GEMINI_API_KEY")

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

    try:
        async with httpx.AsyncClient(timeout=30) as client:
            r = await client.post(GEMINI_URL, json=payload)
            # If non-200, include response body to help debugging
            if r.status_code >= 400:
                raise HTTPException(status_code=502, detail=f"Upstream error from Gemini: {r.status_code} {r.text}")
            data = r.json()
    except httpx.RequestError as e:
        raise HTTPException(status_code=502, detail=f"Network error calling Gemini: {e}")

    # Extract text safely
    text = ""
    try:
        text = data["candidates"][0]["content"]["parts"][0]["text"]
    except Exception:
        raise HTTPException(status_code=502, detail="Unexpected response format from Gemini")

    # Very light parser: expect the model to produce blocks.
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
            flush_current()
            current = {"title": line.split(":",1)[1].strip(), "ingredients": [], "steps": []}
        elif low.startswith("ingredients"):
            items = [x.strip("-• ").strip() for x in line.split("\n")[1:]]
            current["ingredients"].extend([i for i in items if i])
        elif low.startswith("steps"):
            steps = [x.strip("-• ").strip() for x in line.split("\n")[1:]]
            current["steps"].extend([s for s in steps if s])
        else:
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
