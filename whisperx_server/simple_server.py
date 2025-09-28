#!/usr/bin/env python3
"""
Simple Mock WhisperX Server for Testing
Simulates ultra-fast speech recognition for voice commands
"""

import os
import json
import tempfile
from flask import Flask, request, jsonify
from flask_cors import CORS
import logging

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = Flask(__name__)
CORS(app)

class MockWhisperXProcessor:
    def __init__(self):
        logger.info("Initializing Mock WhisperX processor")

    def transcribe_audio(self, audio_file_path):
        """Mock transcription - detects audio file size/duration for 'update' simulation"""
        try:
            # Check if file exists and has content
            if not os.path.exists(audio_file_path):
                return ""

            file_size = os.path.getsize(audio_file_path)
            logger.info(f"Processing audio file: {file_size} bytes")

            # Mock transcription based on file size
            # For testing - return "update" for files > 10KB (typical for 800ms audio)
            if file_size > 10000:  # ~10KB
                return "update please"
            elif file_size > 5000:   # ~5KB
                return "hello update"
            elif file_size > 1000:   # ~1KB
                return "update"
            else:
                return "noise"

        except Exception as e:
            logger.error(f"Mock transcription error: {str(e)}")
            return ""

# Initialize processor
processor = MockWhisperXProcessor()

@app.route('/health', methods=['GET'])
def health_check():
    """Health check endpoint"""
    return jsonify({
        "status": "healthy",
        "model": "mock-whisperx-tiny",
        "note": "This is a mock server for testing"
    })

@app.route('/transcribe', methods=['POST'])
def transcribe():
    """Transcribe audio file"""
    try:
        if 'file' not in request.files:
            return jsonify({"error": "No audio file provided"}), 400

        audio_file = request.files['file']
        if audio_file.filename == '':
            return jsonify({"error": "No file selected"}), 400

        # Save uploaded file temporarily
        with tempfile.NamedTemporaryFile(delete=False, suffix='.wav') as temp_file:
            audio_file.save(temp_file.name)
            temp_path = temp_file.name

        try:
            # Mock transcribe audio
            transcription = processor.transcribe_audio(temp_path)

            logger.info(f"Mock Transcribed: '{transcription}'")

            return jsonify({
                "text": transcription,
                "success": True
            })

        finally:
            # Clean up temp file
            if os.path.exists(temp_path):
                os.unlink(temp_path)

    except Exception as e:
        logger.error(f"Mock transcription endpoint error: {str(e)}")
        return jsonify({"error": str(e), "success": False}), 500

@app.route('/command', methods=['POST'])
def detect_command():
    """Optimized endpoint for command detection - MOCK VERSION"""
    try:
        if 'file' not in request.files:
            return jsonify({"error": "No audio file provided"}), 400

        audio_file = request.files['file']

        # Save uploaded file temporarily
        with tempfile.NamedTemporaryFile(delete=False, suffix='.wav') as temp_file:
            audio_file.save(temp_file.name)
            temp_path = temp_file.name

        try:
            # Mock transcribe audio
            transcription = processor.transcribe_audio(temp_path)

            # Check for update command (case insensitive)
            is_update_command = "update" in transcription.lower()

            logger.info(f"Mock Command detection - Text: '{transcription}' -> Update: {is_update_command}")

            return jsonify({
                "text": transcription,
                "is_update_command": is_update_command,
                "success": True,
                "mock": True
            })

        finally:
            # Clean up temp file
            if os.path.exists(temp_path):
                os.unlink(temp_path)

    except Exception as e:
        logger.error(f"Mock command detection error: {str(e)}")
        return jsonify({"error": str(e), "success": False}), 500

if __name__ == '__main__':
    print("Starting Mock WhisperX Server for Testing...")
    print("This server simulates WhisperX responses for development")
    print("Server running on http://localhost:5001")
    app.run(host='0.0.0.0', port=5001, debug=True)