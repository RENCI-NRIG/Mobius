FROM python:3
MAINTAINER Komal Thareja<komal.thareja@gmail.com>
WORKDIR /code
VOLUME ["/code"]
ARG databasehost=database
ARG database=mobius
ARG user=mobius
ARG password=mobius
ARG mobiushost=mobius
ARG kafkahost
ARG tc=15
ARG td=85
ARG tm=85
ARG bucketcount=10
ARG leasedays=2 
ADD monitoring-py /code/monitoring-py
RUN pip install psutil
RUN pip install Kafka-python
RUN pip install psycopg2 
RUN pip install requests 
RUN cd /code/monitoring-py; python setup.py install; cd /code/
CMD ["sleep", "324"]
#CMD ["python", "/usr/local/bin/monitoringd", "start", "-dh", $databasehost, "-d", $database, "-u", $user, "-p", $password, "-m", $mobiushost, "-k", $kafkahost, "-tc", $tc, "-td", $td, "-tm", $tm, "-b", $bucketcount, "-l", $leasedays]
