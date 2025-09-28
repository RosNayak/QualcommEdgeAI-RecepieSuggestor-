# Recipe LLM Backend (FastAPI + Render)

A tiny FastAPI backend that hides your model API key, exposes `/recipes`, and deploys cleanly on Render.

## Endpoints

- `GET /health` – simple healthcheck
- `POST /recipes` – body:
```json
{
  "ingredients": ["tomato", "onion", "eggs"],
  "servings": 2,
  "dietary": ["vegetarian"]
}
```

Returns:
```json
{
  "recipes": [{"title":"...", "ingredients":[...], "steps":[...]}],
  "model": "gemini-1.5-flash",
  "provider": "gemini"
}
```

## Local Dev

1. `python -m venv .venv && source .venv/bin/activate`
2. `pip install -r requirements.txt`
3. Copy `.env.example` to `.env` and set `GEMINI_API_KEY`
4. `uvicorn app.main:app --reload`

## Deploy on Render (Blueprint)

- Push this repo to GitHub
- On Render: New ➜ **Blueprint** ➜ point to repo with `render.yaml`
- Set `GEMINI_API_KEY` in the web service’s Environment tab

## Android usage

Send a POST to `<your-base-url>/recipes` with header `Authorization: Bearer <CLIENT_TOKEN>` and JSON body as above.
