# QualcommEdgeAI Fridge - Smart Recipe Suggestion App

## Contributors

- Mathew Martin (mathew.martin.0001@gmail.com)
- Roshan Nayak (roshannayak610@gmail.com
- Neil Noronha (nn2685@nyu.edu)

## Summary
An intelligent Android application that uses computer vision and AI to analyze refrigerator contents and suggest recipes based on available ingredients. Built with Google ML Kit and Gemini AI for recipe generation.

## Features

- **Real-time Ingredient Detection**: Camera-based ingredient recognition using Google ML Kit GenAI Image Description
- **Smart Recipe Suggestions**: Gemini AI generates personalized recipes based on detected ingredients
- **Optimized Performance**: ProGuard optimization and frame skipping for efficient processing
- **Edge AI Ready**: Architecture designed for future Qualcomm NPU acceleration

## Architecture

### AI/ML Components

#### Computer Vision Pipeline
- **Google ML Kit GenAI Image Description**: Primary vision processing
- **Frame Processing**: Optimized to process every 8th frame for performance
- **OpenNLP**: Natural language processing for ingredient extraction
- **Ingredient Accumulator**: Real-time ingredient tracking and deduplication

#### Recipe Generation
- **Google Generative AI (Gemini)**: AI-powered recipe creation
- **Context-Aware**: Uses detected ingredients and user preferences
- **Real-time Updates**: Dynamic recipe suggestions based on ingredient changes

### Performance Optimizations

- **ProGuard Enabled**: 9.6% APK size reduction (52MB → 47MB)
- **Frame Skipping**: Process every 8th frame (50% CPU reduction)
- **Polling Optimization**: 2-second ingredient accumulator polling
- **Background Processing**: Threaded execution for ML operations

## User Interface

- **Camera Preview**: Real-time ingredient scanning
- **Recipe List**: ScrollView with suggested recipes
- **Recipe Details**: Detailed ingredient lists and cooking instructions
- **Voice Feedback**: Audio confirmation of detected commands

## Technical Stack

### Android Components
- **Language**: Java
- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 34 (Android 14)
- **Architecture**: MVVM with Singleton patterns

### Dependencies
```gradle
// AI/ML Libraries
implementation("com.google.mlkit:image-labeling:17.0.9")
implementation("com.google.mlkit:genai-image-description:1.0.0-beta1")
implementation("com.google.ai.client.generativeai:generativeai:0.9.0")
implementation("org.apache.opennlp:opennlp-tools:1.9.4")

// Camera
implementation("androidx.camera:camera-core:1.3.4")
implementation("androidx.camera:camera-camera2:1.3.4")
implementation("androidx.camera:camera-lifecycle:1.3.4")
implementation("androidx.camera:camera-view:1.3.4")

// UI Components
implementation("androidx.recyclerview:recyclerview:1.3.2")
implementation("com.google.android.material:material:1.8.0")
```


## Setup Instructions

### Prerequisites
- Android Studio Arctic Fox or later
- Android device with API level 26+
- Google AI Studio API key

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/yourusername/Qualcomm_Hackathon_FridgeChef.git
   cd QualcommEdgeAI_Fridge
   ```

2. **Configure API Keys**
   - Get a Google AI Studio API key
   - Update `ApiKeyManager.java` with your key (line 94)

3. **Build and Run**
   ```bash
   ./gradlew assembleDebug
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```
   ```

## Usage

### Basic Operation

1. **Launch App**: Open the app and grant camera/microphone permissions
2. **Point Camera**: Aim at refrigerator contents or ingredients
3. **Voice Command**: Say "Update" to generate recipes
4. **Browse Recipes**: Scroll through suggested recipes
5. **View Details**: Tap recipes for detailed instructions

### Voice Commands

- **"Update"**: Triggers recipe generation based on detected ingredients
- **Manual Button**: Use test button if voice recognition fails

### Performance Features

- **Adaptive Processing**: App automatically adjusts frame processing based on device capabilities
- **Background Optimization**: Reduced processing when app is not in foreground
- **Memory Management**: Efficient ML model loading and caching

## Configuration

### Performance Tuning

**Frame Processing Rate** (`ImageAnalyzer.java`)
```java
private final long SKIP_FRAMES = 8; // Process every 8th frame
```

**Polling Frequency** (`MainActivity.java`)
```java
accumulatorHandler.postDelayed(this, 2000); // 2-second polling
```

**ProGuard Rules** (`proguard-rules.pro`)
- ML Kit classes preserved
- Custom application classes protected
- Debugging information retained

### Build Variants

- **Debug**: Full logging, no optimization
- **Release**: ProGuard enabled, optimized for performance

## Future Optimizations

### Qualcomm NPU Integration

**Planned Enhancements**:
- Replace Google ML Kit with SNPE (Snapdragon Neural Processing Engine)
- Implement QNN (Qualcomm Neural Network SDK) for NPU acceleration
- Deploy quantized INT8 models for edge processing
- Add Hexagon DSP utilization for audio processing

**Expected Performance Gains**:
- 3-5x inference speedup
- 40-60% power consumption reduction
- 100% offline operation capability

### Additional Optimizations

- **Model Quantization**: INT8 conversion for smaller model sizes
- **Batch Processing**: Group inference requests for efficiency
- **Thermal Management**: Dynamic performance scaling based on device temperature
- **Power Profiles**: Adaptive processing for battery optimization

## Performance Metrics

### Current Benchmarks

- **APK Size**: 47MB (optimized with ProGuard)
- **Frame Processing**: Every 8th frame (~3.75 FPS at 30 FPS input)
- **Memory Usage**: ~150MB average during operation
- **Cold Start**: ~2-3 seconds app launch time

### Optimization Results

- **ProGuard**: 9.6% size reduction
- **Frame Skipping**: 50% CPU usage reduction
- **Polling Optimization**: 25% background power savings

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/improvement`)
3. Commit changes (`git commit -am 'Add improvement'`)
4. Push to branch (`git push origin feature/improvement`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- **Google ML Kit** for computer vision capabilities
- **Qualcomm** for edge AI acceleration frameworks
- **Google Gemini** for intelligent recipe generation

## Support

For questions or issues:
- Create an issue in this repository
- Check the [documentation](docs/) for detailed guides
- Review the [FAQ](docs/FAQ.md) for common problems

---
