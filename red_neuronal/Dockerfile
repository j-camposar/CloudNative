FROM public.ecr.aws/lambda/python:3.10

# Actualizar pip, setuptools y wheel a su última versión para soportar binarios modernos
RUN pip install --no-cache-dir --upgrade pip wheel setuptools

# Instalar TensorFlow, NumPy y Boto3 forzando el uso exclusivo de paquetes binarios pre-compilados (--only-binary=:all:)
RUN pip install --no-cache-dir --only-binary=:all: numpy tensorflow boto3

# Copiar el archivo principal al directorio de tareas de la Lambda
COPY main.py ${LAMBDA_TASK_ROOT}

# Definir el controlador principal
CMD [ "main.lambda_handler" ]