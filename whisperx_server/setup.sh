#!/bin/bash
# WhisperX Server Setup Script

echo "Setting up WhisperX Local Server..."

# Create virtual environment
python3 -m venv whisperx_env

# Activate virtual environment
source whisperx_env/bin/activate

# Upgrade pip
pip install --upgrade pip

# Install requirements
pip install -r requirements.txt

echo "Setup complete!"
echo "To start the server:"
echo "1. source whisperx_env/bin/activate"
echo "2. python server.py"