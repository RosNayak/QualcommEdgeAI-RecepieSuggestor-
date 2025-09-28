from pydantic import BaseModel, Field
from typing import List, Optional

class RecipeRequest(BaseModel):
    ingredients: List[str] = Field(default_factory=list, description="List of detected ingredients")
    servings: Optional[int] = Field(default=2, ge=1, le=12)
    dietary: Optional[List[str]] = Field(default=None, description="e.g., ['vegetarian','gluten-free']")

class Recipe(BaseModel):
    title: str
    ingredients: List[str]
    steps: List[str]

class RecipeResponse(BaseModel):
    recipes: List[Recipe]
    model: str
    provider: str
