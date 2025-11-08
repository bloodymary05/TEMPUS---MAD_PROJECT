import os
from mistralai import Mistral

# Initialize Mistral client from env var; fallback uses existing default in codebase
MISTRAL_API_KEY = os.getenv("MISTRAL_API_KEY", "NHA20RVDtX2U1Paz7glFGrjN6ODHBFn7")
client = Mistral(api_key=MISTRAL_API_KEY)

def get_client():
    """Return the initialized Mistral client."""
    return client
