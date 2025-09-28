import os
from fastapi import FastAPI, Depends, HTTPException, status
from fastapi.middleware.cors import CORSMiddleware
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer

from app.schemas import RecipeRequest, RecipeResponse, Recipe
from app.providers import recipes_from_gemini

CLIENT_TOKEN = os.getenv("CLIENT_TOKEN", "")  # short shared secret with your app

app = FastAPI(title="Recipe LLM Backend", version="0.1.0")

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
