kubectl delete deployment chinaski

docker rmi chinaski:1.0

docker build -t chinaski:1.0 .

cd infra

kubectl apply -f deployment.yml