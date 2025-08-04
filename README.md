# DevSecOps Mega Project-Springboot Bankapp

## End-to-End Bank Application Deployment using DevSecOps on AWS EKS
- This is a multi-tier bank an application written in Java (Springboot).

  
## Step 1: Create an IAM User with Administrator Permissions

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

If you are on Windows, refer to this [guide](https://amitabhdevops.hashnode.dev/a-step-by-step-guide-to-adding-ubuntu-wsl-in-vs-code-terminal) to integrate an Ubuntu terminal in VSCode for seamless project execution.

---

## Step 3: Fork and Clone the Project Repository

1. **Fork the Repository:**
    
    * Open the repository [DevOps Mega Project](https://github.com/Amitabh-DevOps/DevOps-mega-project) on GitHub.
        
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

## Step 4: Install AWS CLI and Configure It

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

## Step 5: Build and Push Docker Image

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

## Step 6: Set Up Infrastructure with Terraform

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

Follow this [guide](https://amitabhdevops.hashnode.dev/how-to-install-essential-devops-tools-on-ubuntulinux) to install:

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

## Step 9: Set Up ArgoCD

#### Step 1: Create a Namespace for ArgoCD

To ensure ArgoCD has its own isolated environment within your Kubernetes cluster, create a dedicated namespace.

```bash
kubectl create ns argocd
```

---

#### Step 2: Install ArgoCD

Use the official installation manifest from ArgoCD’s GitHub repository to deploy it to your cluster.

```bash
kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml
```

This command installs all required ArgoCD components in the `argocd` namespace.

---

#### Step 3: Install ArgoCD CLI

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

![image](https://github.com/user-attachments/assets/32222a1f-3aea-450b-a7e5-0f44b34702ed)


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

#### Step 11: Add Project Repository in ArgoCD UI

To integrate your Git repository with ArgoCD:

1. Navigate to **Settings** &gt; **Repositories** in the ArgoCD UI.
    
2. Click on **Connect Repo** and provide the appropriate repository URL.
    
3. Select the connection method as HTTPS. If the repository is private:
    
    * Enter your username and password to authenticate.
        
    * Otherwise, skip the authentication step for public repositories.
        
4. Choose the default project (or any specific project, if configured) and complete the setup.
    

Once connected, your repository will be ready for deploying applications via ArgoCD.

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

### 2\. Install Ingress Controller Using Helm

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

For securing your application with **HTTPS** using your custom domain name, you need to generate SSL/TLS certificates. **Cert-Manager** is a Kubernetes tool that automates the management and issuance of these certificates.

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

![image](https://github.com/user-attachments/assets/c264ccbc-9ba7-40e1-8925-47e48ec65b26)


![image](https://github.com/user-attachments/assets/dbabd258-d314-4648-b17a-f17bdb1ec55d)


---

## Step 12: Exposing the Application via Ingress or NodePort

In this step, we will walk through two options to expose your application to the outside world: one using an **ALB** (Application Load Balancer) with a CNAME record and the other using **NodePort** if you don't have a domain.

---

### Option 1: Expose via ALB and CNAME

1. Run the following command to get the **ALB External-IP** of the `ingress-nginx-controller`:
    
    ```bash
    kubectl get svc -n ingress-nginx
    ```
    
2. Copy the **External-IP** from the output and create a **CNAME record** on your domain. For example, use [`amitabh.letsdeployit.com`](http://amitabh.letsdeployit.com) as your domain, and replace it in the `bankapp-ingress.yml` file with your domain name, take a reference of below image:
    
    1. ![image](https://github.com/user-attachments/assets/117c42ad-025c-4ad7-af88-064ddbfc86b4)

        
3. After updating the `bankapp-ingress.yml` file, sync the application in **ArgoCD**.
    
4. Once synchronized, open your browser and access the application via your domain (e.g., [`amitabh.letsdeployit.com`](http://amitabh.letsdeployit.com)).
    
    1. ![image](https://github.com/user-attachments/assets/8c7076dc-2908-45a9-a920-86ff281e2a4b)

        
    2. ![image](https://github.com/user-attachments/assets/ab5e3652-5296-4744-bc38-1ec2fd7a32a7)

        

---

### Option 2: Expose via NodePort

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
    
4. Now, access your application in the browser using the URL format: `http://<worker_node_public_ip>:<nodeport>`.
    
    ![image](https://github.com/user-attachments/assets/1414aff3-7137-4c3c-b043-f63d964eee54)

    
    ![image](https://github.com/user-attachments/assets/bc9fff7e-5aee-40f5-963f-5683e9dc1a8f)

    

---

## Step 13: Setting Up Jenkins for Continuous Integration.

### 1\. Install Jenkins on the Master Node

Install **Jenkins** on the master node by following this blog: [How to Install Essential DevOps Tools on Ubuntu Linux](https://amitabhdevops.hashnode.dev/how-to-install-essential-devops-tools-on-ubuntulinux).

After installation, open port **8080** on the master node and access Jenkins in your browser:

```bash
http://<master-node-public-ip>:8080
```

Complete the Jenkins setup by following the on-screen instructions to configure the admin username and password.

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

Add your **DockerHub** credentials to Jenkins. You can refer to this blog for detailed steps: [Django Notes App using Jenkins CI/CD](https://amitabhdevops.hashnode.dev/django-notes-app-using-jenkins-cicd#heading-step-12-set-up-docker-hub-credentials-in-jenkins).

---

### 4\. Add GitHub Credentials

Add **GitHub** credentials to Jenkins as well to enable seamless integration with your GitHub repository.

---

### 5\. Set Up Webhook for Continuous Integration

To automatically trigger Jenkins builds on changes in your GitHub repository, set up a webhook. Follow the instructions in this blog: [Set Up Webhooks for Automatic Deployment](https://amitabhdevops.hashnode.dev/django-notes-app-using-jenkins-cicd#heading-step-13-set-up-webhooks-for-automatic-deployment).

---

### 6\. Create a Jenkins Pipeline Job

Create a **Jenkins Pipeline** job using the reference in this blog: [Create a Jenkins Pipeline Job](https://amitabhdevops.hashnode.dev/django-notes-app-using-jenkins-cicd#heading-step-10-create-a-jenkins-pipeline-job).

While creating the job, ensure that you check the box for **This project is parameterized** to allow dynamic configuration during the build.

---

### 7\. Build the Pipeline

Once everything is set up, trigger the pipeline build by selecting **Build with Parameters**. Enter the required parameters and start the build process. Monitor the build logs for any errors. If any issues arise, resolve them.

* Check the **Docker Hub** for the tagged images after the build.
    
* Ensure that the **bankapp-deployment** is using the correct image tag from **Docker Hub**. take a reference of below image
    
    ![image](https://github.com/user-attachments/assets/4600f18f-c57d-4e40-b517-4d9507f09a0b)

    
    
* ![image](https://github.com/user-attachments/assets/c1db5a21-1f22-49c6-b674-5604efb8dc2f)

    

![image](https://github.com/user-attachments/assets/cfd5988d-471d-481d-9bf9-f0581c4b7d98)


![image](https://github.com/user-attachments/assets/b0c815ec-9960-443d-95e7-69511b462ee6)


![image](https://github.com/user-attachments/assets/4135926b-b89b-4202-813c-3ecf387e0475)


![image](https://github.com/user-attachments/assets/fe09aa64-a777-4a8e-9da4-b999a3734119)


---

## Step 14: Setting Up Observability with Prometheus and Grafana

### 1\. Add Prometheus Helm Repository

Start by adding the **Prometheus** Helm repository:

```bash
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
```

---

### 2\. Create the Prometheus Namespace

Create a dedicated namespace for Prometheus:

```bash
kubectl create namespace prometheus
```

---

### 3\. Install Prometheus

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

### 5\. Expose Grafana via NodePort

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

![image](https://github.com/user-attachments/assets/76e48681-ee88-4c10-a231-60ecbeab836c)


![image](https://github.com/user-attachments/assets/0bea99b0-1316-4076-b823-c4dc5fb4a180)


![image](https://github.com/user-attachments/assets/c06f10e7-9a4f-420b-8586-c172a44b1a96)


![image](https://github.com/user-attachments/assets/e0fb4173-00aa-479d-b1ad-9d36c42c7ee4)


---

## Conclusion

In conclusion, your DevSecOps Mega Project showcases a well-structured and automated pipeline using industry-standard tools. You've effectively integrated AWS, Docker, Kubernetes (EKS), Helm, and ArgoCD for deployment automation. By leveraging Terraform for infrastructure as code and implementing security best practices like IAM roles, SSL certificates, and Horizontal Pod Autoscaling, your setup ensures a secure, scalable, and efficient environment. The project demonstrates strong knowledge in cloud infrastructure, containerization, and CI/CD practices, positioning you well for real-world DevSecOps implementation.

---

# <mark>Project ended here.</mark>

below is for opensource opportunity for contrubutors.

---

## Open Source Opportunity: Contribute and Win Free Gift Hampers from TrainWithShubham

We are thrilled to announce an exciting **open-source opportunity** for contributors to this project. As a token of appreciation, contributors will receive **Free Gift Hampers** from **TrainWithShubham**. The hampers may include a variety of items to make your contribution even more rewarding.

## How You Can Contribute

We are looking for contributions in the following areas:

---

### 1\. **Solve the Application Login Issue After Domain Mapping**

After mapping the domain to the application, there may be issues with user login. If you have experience in troubleshooting or resolving login issues post-domain mapping, we encourage you to contribute a solution. Possible areas for improvement include authentication, DNS configurations, or session management.

### Resolved:

https://github.com/user-attachments/assets/4baf7031-5744-40c5-8035-10e4d1cc4457


---

### 2\. **Add the Security (Sec) Part of DevSecOps to the Project**

Security should be a priority for any application. By incorporating **DevSecOps** practices into the project, we can significantly improve its security. Contributions may involve:

* Implementing automated security scans within the CI/CD pipeline.
    
* Integrating tools like **OWASP ZAP**, **Trivy**, or **SonarQube** for vulnerability assessments.
    
* Securing Kubernetes, Docker, and application infrastructure.
    

---

### 3\. **Further Improvements**

We are always looking for ways to improve the project. If you have ideas for additional features or optimizations, we would love to see them:

* Enhancing performance and scalability.
    
* Improving monitoring and logging capabilities.
    
* Streamlining deployment and automation processes.
    
* Improving user experience and interface design.
    

---

## How to Contribute

To contribute, simply **fork the repository**, make your changes, and submit a **pull request (PR)**. If your contribution is accepted, you will receive a **Free Gift Hamper** from **TrainWithShubham** as a thank-you for your efforts.

---

## Join the Open-Source Community

We welcome all developers, whether beginners or experienced, to join the open-source community and contribute to this project. Let’s work together to make this project even better!

We look forward to your innovative contributions, and remember, great work deserves great rewards!
