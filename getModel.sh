mkdir -p app/src/main/assets/models

curl -L \
  -o app/src/main/assets/models/efficientdet_lite2_int8.tflite \
  "https://tfhub.dev/tensorflow/lite-model/efficientdet/lite2/detection/metadata/1?lite-format=tflite"
