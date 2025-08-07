# DevSecOps Mega Project-Springboot Bankapp

## End-to-End Bank Application Deployment using DevSecOps on AWS EKS
- This is a multi-tier bank an application written in Java (Springboot).

  
## Step 1: Creating an IAM User with Administrator Permissions

1. **Login to AWS Console:** Open the [AWS Management Console](https://aws.amazon.com/console/).
    
2. **Navigate to IAM:** Go to the Identity and Access Management (IAM) service.
    
3. **Create User:**
    
    * Click on **Users** &gt; **Add Users**.
        
    * Enter a username, e.g., `mega-project-user`.
        
    * Select **Programmatic access** to generate an access key.
        
4. **Attach Permissions:** Attach the policy `AdministratorAccess`.
    
5. **Generate Access Key:**
    
    * In the Security tab, create an access key.
        
    * Save the **Access Key ID** and **Secret Access Key** securely.
        

---

## Step 2: Setting Up Visual Studio Code (VSCode)

### Adding Linux Terminal in VSCode (Windows Users)

---

## Step 3: Fork and Clone the Project Repository

1. **Fork the Repository:**
    
    * Open the repository on GitHub.
        
    * Click **Fork** to create a copy in your GitHub account.
        
2. **Clone the Repository:**
    
    * Open the terminal in VSCode.
        
    * Clone the repository:
        
        ```bash
        git clone https://github.com/<your-username>/DevOps-mega-project.git
        ```
        
    * Switch to the project branch:
        
        ```bash
        git checkout project
        ```
        

---

## Step 4: Installing AWS CLI and Configure It

1. **Install AWS CLI:**
    
    ```bash
    sudo apt update
    sudo apt install awscli -y
    ```
    
2. **Configure AWS CLI:**
    
    ```bash
    aws configure
    ```
    
    * Enter the Access Key ID and Secret Access Key.
        
    * Specify your preferred AWS region (e.g., `eu-west-1`).
        

---

## Step 5: Building and Pushing Docker Image

1. **Build the Docker Image:**
    
    ```bash
    docker build -t <dockerhub-username>/bankapp:latest .
    ```
    
2. **Login to DockerHub:**
    
    ```bash
    docker login
    ```
    
3. **Push the Image to DockerHub:**
    
    ```bash
    docker push <dockerhub-username>/bankapp:latest
    ```
    
4. **Update Deployment File:**
    
    * Update the `bankapp-deployment.yml` file to use your Docker image.
        

---

## Step 6: Setting Up Infrastructure with Terraform

1. **Generate SSH Key:**
    
    ```bash
    ssh-keygen
    ```
    
    * Enter a name as `mega-project-key`.
        
    * Update the `variable.tf` file with the key name , if you have entered another key name.
        
2. **Initialize Terraform**: Run the initialization command to download provider plugins and prepare your working directory:
    
    ```bash
    terraform init
    ```
    
3. **Plan Terraform Execution**: Preview the resources Terraform will create:
    
    ```bash
    terraform plan
    ```
    
4. **Apply Terraform Configuration**: Deploy the infrastructure using:
    
    ```bash
    terraform apply --auto-approve
    ```
    
5. **Connect to EC2 Instance**: Once the infrastructure is created, connect to your EC2 instance:
    
    ```bash
    ssh -i mega-project-key.pem ubuntu@<instance-public-ip>
    ```
    

---

## Step 7: Install Essential DevOps Tools on created Instance.

* AWS CLI
    
* kubectl
    
* eksctl
    

### Install eksctl:

```bash
ARCH=amd64
PLATFORM=$(uname -s)_$ARCH

curl -sLO "https://github.com/eksctl-io/eksctl/releases/latest/download/eksctl_$PLATFORM.tar.gz"
tar -xzf eksctl_$PLATFORM.tar.gz -C /tmp && rm eksctl_$PLATFORM.tar.gz
sudo mv /tmp/eksctl /usr/local/bin
```

---

## Step 8: Create Kubernetes Cluster

1. **Create EKS Cluster:**
    
    ```bash
    eksctl create cluster --name bankapp-cluster --region eu-west-1 --without-nodegroup
    ```
    
2. **Verify Cluster Creation:**
    
    ```bash
    eksctl get clusters
    ```
    
3. **Associate IAM OIDC Provider:**
    
    ```bash
    eksctl utils associate-iam-oidc-provider --region=eu-west-1 --cluster=bankapp-cluster --approve
    ```
    
4. **Create Node Group:**
    
    ```bash
    eksctl create nodegroup \
      --cluster=bankapp-cluster \
      --region=eu-west-1 \
      --name=bankapp-ng \
      --node-type=t2.medium \
      --nodes=2 \
      --nodes-min=2 \
      --nodes-max=2 \
      --node-volume-size=15 \
      --ssh-access \
      --ssh-public-key=mega-project-key
    ```
    

---

## Step 9: Setting Up ArgoCD

#### Step 1: Create a Namespace for ArgoCD

To ensure ArgoCD has its own isolated environment within your Kubernetes cluster, create a dedicated namespace.

```bash
kubectl create ns argocd
```

---

#### Step 2: Installing ArgoCD

Use the official installation manifest from ArgoCD’s GitHub repository to deploy it to your cluster.

```bash
kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml
```

This command installs all required ArgoCD components in the `argocd` namespace.

---

#### Step 3: Installing ArgoCD CLI

To interact with the ArgoCD server from your local machine or a terminal, install the ArgoCD command-line interface (CLI).

```bash
curl -sSL -o argocd-linux-amd64 https://github.com/argoproj/argo-cd/releases/latest/download/argocd-linux-amd64
sudo install -m 555 argocd-linux-amd64 /usr/local/bin/argocd
rm argocd-linux-amd64
```

Once installed, verify the installation using:

```bash
argocd version
```
<img width="1852" height="832" alt="added git repo secessfully" src="https://github.com/user-attachments/assets/8d09e6c9-0905-41cb-bd6b-09b69fd80c58" />
<img width="1918" height="910" alt="added git repo secessfully-2" src="https://github.com/user-attachments/assets/dea37b81-31fb-4860-9946-9dfb2bc1ce0c" />

---

#### Step 4: Check ArgoCD Services

To confirm that ArgoCD services are running:

```bash
kubectl get svc -n argocd
```

This lists all services in the `argocd` namespace. Take note of the `argocd-server` service, as it will be exposed in the next step.

---

#### Step 5: Expose ArgoCD Server Using NodePort

By default, the `argocd-server` service is of type `ClusterIP`, which makes it accessible only within the cluster. Change it to `NodePort` to expose it externally.

```bash
kubectl patch svc argocd-server -n argocd -p '{"spec":{"type": "NodePort"}}'
```

Retrieve the updated service information to identify the assigned NodePort:

```bash
kubectl get svc -n argocd
```

Note the port in the `PORT(S)` column (e.g., `30529`).

---

#### Step 6: Configure AWS Inbound Rule for NodePort

If your Kubernetes cluster is hosted on AWS, ensure that the assigned NodePort is accessible by adding an inbound rule to your security group. Allow traffic on this port from the internet to the worker node(s).

---

#### Step 7: Access ArgoCD Web UI

With the NodePort and the worker node’s public IP, access the ArgoCD web UI:

```bash
http://<worker-node-public-ip>:<node-port>
```

<img width="1920" height="1080" alt="agrocd ui" src="https://github.com/user-attachments/assets/cfe7d3de-4fa1-4259-b1fb-fe7a7f1eda23" />

For the initial login:

* **Username:** `admin`
    
* **Password:** Retrieve using the following command:
    

```bash
kubectl get secret argocd-initial-admin-secret -n argocd -o jsonpath="{.data.password}" | base64 -d
```

Change the password after logging in by navigating to the user info section in the ArgoCD UI.

---

#### Step 8: Log In to ArgoCD via CLI

To log in from the CLI, use the public IP and NodePort:

```bash
argocd login <worker-node-public-ip>:<node-port> --username admin
```

For example:

```bash
argocd login 54.154.41.147:30529 --username admin
```

---

#### Step 9: Check ArgoCD Cluster Configuration

To view the cluster configurations managed by ArgoCD:

```bash
argocd cluster list
```

---

#### Step 10: Add a Cluster to ArgoCD

If your cluster is not already added, first identify its context:

```bash
kubectl config get-contexts
```

Then, add the desired cluster to ArgoCD. Replace the placeholders with your actual cluster context and name:

```bash
argocd cluster add <kube-context> --name <friendly-name>
```

For example:

```bash
argocd cluster add mega-project-user@bankapp-cluster.eu-west-1.eksctl.io --name bankapp-cluster
```

#### Step 11: Adding Project Repository in ArgoCD UI

To integrate your Git repository with ArgoCD:

1. Navigate to **Settings** &gt; **Repositories** in the ArgoCD UI.
    
2. Click on **Connect Repo** and provide the appropriate repository URL.
    
3. Select the connection method as HTTPS. If the repository is private:
    
    * Enter your username and password to authenticate.
        
    * Otherwise, skip the authentication step for public repositories.
        
4. Choose the default project (or any specific project, if configured) and complete the setup.
    

Once connected, your repository will be ready for deploying applications via ArgoCD.
<img width="1850" height="842" alt="adding repo in agrocd-1" src="https://github.com/user-attachments/assets/94a7fedc-b137-4d08-a4ac-36484466f34f" />

<img width="1847" height="823" alt="adding repo in agrocd-2" src="https://github.com/user-attachments/assets/9c66755f-0baf-40a9-bc7c-a9c3e621b996" />

<img width="1852" height="832" alt="added git repo secessfully" src="https://github.com/user-attachments/assets/a232b26c-d5cc-448d-9f55-a61a037f9226" />

<img width="1918" height="910" alt="added git repo secessfully-2" src="https://github.com/user-attachments/assets/95ddfd1a-5d46-413c-bdbe-b8f457b0540c" />



---

## Step 10: Installing Helm, Ingress Controller, and Setting Up Metrics for HPA in Kubernetes

### 1\. Install Helm

**Helm** is a powerful Kubernetes package manager that simplifies the deployment and management of applications within your Kubernetes clusters. To get started, follow the steps below to install Helm on your local system:

```bash
# Download the Helm installation script
curl -fsSL -o get_helm.sh https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3

# Change script permissions to make it executable
chmod 700 get_helm.sh

# Run the installation script
./get_helm.sh
```

After running the script, Helm will be installed, and you can start using it to deploy applications to your Kubernetes cluster.

---

### 2\. Installing Ingress Controller Using Helm

An **Ingress Controller** is necessary to manage external HTTP/HTTPS access to your services in Kubernetes. In this step, we will install the NGINX Ingress Controller using Helm.

To install the NGINX Ingress Controller, execute the following commands:

```bash
# Add the NGINX Ingress controller Helm repository
helm repo add ingress-nginx https://kubernetes.github.io/ingress-nginx

# Update the Helm repository to ensure you have the latest charts
helm repo update

# Install the ingress-nginx controller in the ingress-nginx namespace
helm install ingress-nginx ingress-nginx/ingress-nginx --namespace ingress-nginx --create-namespace
```

This command installs the NGINX Ingress controller into your Kubernetes cluster, creating a new namespace called `ingress-nginx`. This Ingress controller will handle routing and load balancing for your services.

---

### 3\. Apply Metrics Server for HPA

To enable **Horizontal Pod Autoscaling (HPA)** in your Kubernetes cluster, the **metrics-server** is required to collect resource usage data like CPU and memory from the pods. HPA scales your application based on these metrics.

Run the following command to apply the **metrics-server**:

```bash
# Install metrics-server to collect resource usage metrics
kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml
```

Once installed, the metrics-server will start collecting data from your Kubernetes nodes and pods, enabling you to configure HPA based on these metrics.

---

### 4\. Install Cert-Manager for SSL/TLS Certificates

For securing application with **HTTPS** using custom domain name, you need to generate SSL/TLS certificates. **Cert-Manager** is a Kubernetes tool that automates the management and issuance of these certificates.

To install Cert-Manager, use the following command:

```bash
# Apply Cert-Manager components to your cluster
kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.16.2/cert-manager.yaml
```

Once installed, Cert-Manager will be responsible for automatically issuing and renewing SSL/TLS certificates for your services. You can then configure Cert-Manager to issue a certificate for your application and configure HTTPS with your domain.

---

## Step 11: Creating an Application on ArgoCD

### 1\. General Section

* **Application Name**: Choose a name for your application.
    
* **Project Name**: Select **default**.
    
* **Sync Policy**: Choose **Automatic**.
    
* Enable **Prune Resources** and **Self-Heal**.
    
* Check **Auto Create Namespace**.
    

---

### 2\. Source Section

* **Repo URL**: Enter the URL of your Git repository.
    
* **Revision**: Select the branch (e.g., `main`).
    
* **Path**: Specify the directory containing your Kubernetes manifests (e.g., `k8s`).
    

---

### 3\. Destination Section

* **Cluster**: Select your desired cluster.
    
* **Namespace**: Use `bankapp-namespace`.
    

---

### 4\. Create the Application

Click **Create** to finish the setup and deploy your application.

<img width="1915" height="921" alt="creating application in agrocd" src="https://github.com/user-attachments/assets/3fb2cc20-6b47-4a03-af7a-d290df6fcbf4" />

<img width="1918" height="956" alt="creating application in agrocd-2" src="https://github.com/user-attachments/assets/b1ade130-3669-4e8f-a7dd-24572d072cdb" />

<img width="1918" height="941" alt="application created sucessfully" src="https://github.com/user-attachments/assets/f6e35555-718f-45e7-93a6-d05631e412b2" />

<img width="1850" height="838" alt="application-1" src="https://github.com/user-attachments/assets/1ceda823-c9ff-431f-99d1-955287a75d0b" />

<img width="1912" height="918" alt="application-2" src="https://github.com/user-attachments/assets/c8ef7fc6-2456-451b-81c8-1d466bad0a9b" />

---

## Step 12: Exposing the Application via Ingress or NodePort

In this step, we will walk through two options to expose your application to the outside world: one using an **ALB** (Application Load Balancer) with a CNAME record and the other using **NodePort** if you don't have a domain.

---

###  Exposing via NodePort

If you don't have a domain, you can expose the service using **NodePort**.

1. Before patching, check the existing services in the `bankapp-namespace`:
    
    ```bash
    kubectl get svc -n bankapp-namespace
    ```
    
2. Patch the **bankapp-service** to expose it via **NodePort**:
    
    ```bash
    kubectl patch svc bankapp-service -n bankapp-namespace -p '{"spec": {"type": "NodePort"}}'
    ```
    
3. After patching, check the service again to get the **NodePort**:
    
    ```bash
    kubectl get svc -n bankapp-namespace
    ```
    
4. Now, access your application in the browser using the URL format: `http://<worker_node_public_ip>:<nodeport>`

    
<img width="1851" height="872" alt="bankapp-deploy" src="https://github.com/user-attachments/assets/74d2ce3a-8d39-4900-a7a5-70d708836896" />

---

## Step 13: Setting Up Jenkins for Continuous Integration.

### 1\. Install Jenkins on the Master Node

Install **Jenkins** on the master node by following this blog: [How to Install Essential DevOps Tools on Ubuntu Linux](https://amitabhdevops.hashnode.dev/how-to-install-essential-devops-tools-on-ubuntulinux).

After installation, open port **8080** on the master node and access Jenkins in your browser:

```bash
http://<master-node-public-ip>:8080
```

Complete the Jenkins setup by following the on-screen instructions to configure the admin username and password.

<img width="1855" height="897" alt="jenkins-1" src="https://github.com/user-attachments/assets/7dd8b281-bb9e-4b81-9262-b4f46b6e1bbf" />

<img width="1851" height="870" alt="jenkins-3" src="https://github.com/user-attachments/assets/d9cf5264-9265-4234-a4b2-9186191ce3e5" />


---

### 2\. Install Docker and Configure User Permissions

To integrate Jenkins with Docker, you need to install **Docker** and add both the current user and the **Jenkins** user to the Docker group:

1. Install Docker (if not already installed).
    
2. Add the current user to the Docker group:
    
    ```bash
    sudo usermod -aG docker $USER && newgrp docker
    ```
    
3. Add the **Jenkins** user to the Docker group:
    
    ```bash
    sudo usermod -aG docker jenkins
    ```
    
4. Restart Jenkins:
    
    ```bash
    sudo systemctl restart jenkins
    ```
    

---

### 3\. Add DockerHub Credentials

<img width="1812" height="845" alt="jenkins-7-dockerhub" src="https://github.com/user-attachments/assets/385e1932-f5f4-42d0-ae2c-55359ed1b7b9" />

---

### 4\. Add GitHub Credentials

Add **GitHub** credentials to Jenkins as well to enable seamless integration with GitHub repository.

---

### 5\. Setting Up Webhook for Continuous Integration

To automatically trigger Jenkins builds on changes in your GitHub repository, set up a webhook.
---

### 6\. Create a Jenkins Pipeline Job

While creating the job, ensure that you check the box for **This project is parameterized** to allow dynamic configuration during the build.

---

### 7\. Building the Pipeline

Once everything is set up, trigger the pipeline build by selecting **Build with Parameters**. Enter the required parameters and start the build process. Monitor the build logs for any errors. If any issues arise, resolve them.

* Check the **Docker Hub** for the tagged images after the build.
    
* Ensure that the **bankapp-deployment** is using the correct image tag from **Docker Hub**. take a reference of below image


<img width="1843" height="867" alt="jenkins-10-creating-pipeline" src="https://github.com/user-attachments/assets/1f28a628-b2d6-49ea-a3da-ccad1df42d69" />

<img width="1861" height="892" alt="jenkins-10-creating-pipeline-2" src="https://github.com/user-attachments/assets/64683429-228d-494f-8d80-45adefe1347b" />

<img width="1848" height="876" alt="jenkins-10-creating-pipeline-3" src="https://github.com/user-attachments/assets/ed16cc44-2229-4f76-97d9-ad04db228e57" />

<img width="1852" height="872" alt="jenkins-10-creating-pipeline-4" src="https://github.com/user-attachments/assets/217959cc-538f-449c-9f18-fa950b6accde" />

<img width="1853" height="842" alt="jenkins-pipeline-created" src="https://github.com/user-attachments/assets/0b1bdd1f-8c9a-4711-8e76-673186324d70" />


---
## Install and configure SonarQube (Master machine)
    docker run -itd --name SonarQube-Server -p 9000:9000 sonarqube:lts-community

<img width="1852" height="862" alt="sonarqube-1" src="https://github.com/user-attachments/assets/72dc3e3d-04e7-4da6-8656-006922795903" />

<img width="1857" height="877" alt="sonarqube-2" src="https://github.com/user-attachments/assets/23b431f8-ba50-4615-976c-371174fa0c2d" />

<img width="1841" height="863" alt="sonarqube-3" src="https://github.com/user-attachments/assets/3ac6fe6e-0c16-45a6-9487-441d2f20ae9b" />

<img width="1848" height="856" alt="sonarqube-4" src="https://github.com/user-attachments/assets/e15edc8f-de3f-43a1-8312-c8d1df9c42df" />

<img width="1852" height="872" alt="sonarqube-5" src="https://github.com/user-attachments/assets/094ad08d-a68c-45a3-bfbb-ef4727ce8fa2" />

<img width="1856" height="850" alt="sonarqube-6" src="https://github.com/user-attachments/assets/b140136f-97be-4815-9ccc-8ce4d1e55bf4" />

<img width="1845" height="865" alt="sonarqube-7" src="https://github.com/user-attachments/assets/a5c0551d-3b96-4b2a-8e10-b1e62ecb4249" />

<img width="1790" height="795" alt="sonarqube-9" src="https://github.com/user-attachments/assets/37a32c2f-0b08-4ec1-b4d5-83b052cd39bc" />

<img width="1848" height="872" alt="sonarqube-10" src="https://github.com/user-attachments/assets/3a8b5b1a-cb47-4a19-aae6-e4eeabc6d686" />

<img width="1850" height="867" alt="sonarqube-webhook" src="https://github.com/user-attachments/assets/45a7f45c-77f1-4de5-be34-d0807cb20f6a" />

<img width="1917" height="912" alt="webhook added sucessfully" src="https://github.com/user-attachments/assets/b3229be6-4c3a-4804-aa83-175ae64a96a9" />


## Install Trivy (Jenkins Worker)
   sudo apt-get install wget apt-transport-https gnupg lsb-release -y
   wget -qO - https://aquasecurity.github.io/trivy-repo/deb/public.key | sudo apt-key add -
   echo deb https://aquasecurity.github.io/trivy-repo/deb $(lsb_release -sc) main | sudo tee -a /etc/apt/sources.list.d/trivy.list
   sudo apt-get update -y
   sudo apt-get install trivy -y



## Step 14: Setting Up Observability with Prometheus and Grafana

### 1\. Adding Prometheus Helm Repository

Start by adding the **Prometheus** Helm repository:

```bash
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
```

---

### 2\. Creating the Prometheus Namespace

Create a dedicated namespace for Prometheus:

```bash
kubectl create namespace prometheus
```

---

### 3\. Installing Prometheus

Install the **Prometheus** and **Grafana** stack using Helm in the `prometheus` namespace:

```bash
helm install stable prometheus-community/kube-prometheus-stack -n prometheus
```

---

### 4\. Get Services in the Prometheus Namespace

To view the services running in the `prometheus` namespace, use the following command:

```bash
kubectl get svc -n prometheus
```

---

### 5\. Exposing Grafana via NodePort

Expose **Grafana** through **NodePort** by patching the service:

```bash
kubectl patch svc stable-grafana -n prometheus -p '{"spec": {"type": "NodePort"}}'
```

Run the following command again to get the **NodePort** and open it in your browser using the **Master Node's Public IP**:

```bash
kubectl get svc -n prometheus
```

---

### 6\. Access Grafana

To access **Grafana**, use the **admin** username and retrieve the password by running the following command:

```bash
kubectl get secret --namespace prometheus stable-grafana -o jsonpath="{.data.admin-password}" | base64 --decode ; echo
```

---

### 7\. Monitoring Your Application

Now that **Prometheus** and **Grafana** are set up, you can use **Grafana** to monitor your application metrics. Grafana will pull metrics from **Prometheus**, and you can create dashboards to visualize various aspects of your application’s performance.

<img width="1918" height="938" alt="grafana" src="https://github.com/user-attachments/assets/66607874-d793-41b6-90aa-ee806981cae2" />

<img width="1853" height="861" alt="grafana-" src="https://github.com/user-attachments/assets/7731b5cc-95e2-468f-9aec-3278eee933d5" />

<img width="1852" height="863" alt="grafana-1" src="https://github.com/user-attachments/assets/eaee187b-04b0-401e-bd70-21aed3511a15" />

<img width="1856" height="871" alt="grafana-2" src="https://github.com/user-attachments/assets/e3b6fc62-bc4f-47f4-9d65-5a0035669f5c" />

---

## Conclusion

In conclusion, your DevSecOps Mega Project showcases a well-structured and automated pipeline using industry-standard tools. You've effectively integrated AWS, Docker, Kubernetes (EKS), Helm, and ArgoCD for deployment automation. By leveraging Terraform for infrastructure as code and implementing security best practices like IAM roles, SSL certificates, and Horizontal Pod Autoscaling, your setup ensures a secure, scalable, and efficient environment. The project demonstrates strong knowledge in cloud infrastructure, containerization, and CI/CD practices, positioning you well for real-world DevSecOps implementation.

---






