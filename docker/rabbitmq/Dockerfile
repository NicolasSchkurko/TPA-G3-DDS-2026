FROM rabbitmq:3-management

# El exchange consistent-hash que usa logisticas NO es parte del core de RabbitMQ: es un
# plugin, y la imagen lo trae apagado.
#
# Por que NO alcanza con RABBITMQ_SERVER_ADDITIONAL_ERL_ARGS=-rabbitmq_plugins [...]:
# esa variable pasa una variable de kernel, pero la imagen YA trae
# /etc/rabbitmq/enabled_plugins con [rabbitmq_management,rabbitmq_prometheus], y ese archivo
# se lee al boot y pisa lo que venga por linea de comandos. Queda todo como estaba y el
# servicio revienta con:
#
#   PRECONDITION_FAILED - unknown exchange type 'x-consistent-hash'
#
# Hay que habilitarlo offline, que es lo que escribe el archivo que si manda.
RUN rabbitmq-plugins enable --offline rabbitmq_consistent_hash_exchange