import os
from fastapi import FastAPI, Depends, HTTPException, status
from fastapi.responses import HTMLResponse
from fastapi.middleware.cors import CORSMiddleware
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer

from app.schemas import RecipeRequest, RecipeResponse, Recipe
from app.providers import recipes_from_gemini

CLIENT_TOKEN = os.getenv("CLIENT_TOKEN", "")  # short shared secret with your app

app = FastAPI(title="Recipe LLM Backend", version="0.1.1")

# CORS: lock to your app's base URL or dev origins
origins = os.getenv("CORS_ORIGINS", "*").split(",")

app.add_middleware(
    CORSMiddleware,
    allow_origins=[o.strip() for o in origins],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

security = HTTPBearer(auto_error=False)

def verify_client_token(creds: HTTPAuthorizationCredentials = Depends(security)):
    if not CLIENT_TOKEN:
        return  # no auth enforced if not set
    if creds is None or creds.scheme.lower() != "bearer" or creds.credentials != CLIENT_TOKEN:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid token")

@app.get("/")
async def root():
    return {
        "service": "Recipe LLM Backend",
        "docs": "/docs",
        "health": "/health",
        "recipes": {"POST": "/recipes"}
    }

@app.get("/health")
async def health():
    return {"status": "ok"}

# Some platforms/proxies expect /healthz; keep it as an alias
@app.get("/healthz")
async def healthz():
    return {"status": "ok"}

@app.get("/recipes")
async def recipes_get_info():
    return {
        "detail": "Use POST /recipes with JSON body and optional Bearer token.",
        "example_body": {
            "ingredients": ["tomato", "onion", "eggs"],
            "servings": 2,
            "dietary": ["vegetarian"]
        }
    }

@app.post("/recipes", response_model=RecipeResponse, dependencies=[Depends(verify_client_token)])
async def recipes(req: RecipeRequest):
    result = await recipes_from_gemini(req.ingredients, req.servings, req.dietary)
    return RecipeResponse(
        recipes=[Recipe(**r) for r in result["recipes"]],
        model=result["model"],
        provider=result["provider"]
    )


@app.get("/recipes/html", response_class=HTMLResponse, dependencies=[Depends(verify_client_token)])
async def recipes_html(ingredients: str):
    """Minimal HTML view.
    Call like: /recipes/html?ingredients=tomato,onion,eggs
    """
    ing_list = [s.strip() for s in ingredients.split(",") if s.strip()]
    result = await recipes_from_gemini(ing_list, servings=2, dietary=None)
    # Build simple HTML
    html_parts = ["<html><head><meta charset='utf-8'><title>Recipes</title></head><body>"]
    html_parts.append(f"<h2>Recipes for: {', '.join(ing_list) or 'none'}</h2>")
    for r in result["recipes"]:
        html_parts.append(f"<h3>{r['title']}</h3>")
        html_parts.append("<strong>Ingredients</strong><ul>")
        for i in r["ingredients"]:
            html_parts.append(f"<li>{i}</li>")
        html_parts.append("</ul>")
        html_parts.append("<strong>Steps</strong><ol>")
        for s in r["steps"]:
            html_parts.append(f"<li>{s}</li>")
        html_parts.append("</ol><hr/>")
    html_parts.append("</body></html>")
    return "\n".join(html_parts)
