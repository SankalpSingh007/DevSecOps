def call(String url, String branch) {
    try {
        git url: url, branch: branch
    } catch (Exception e) {
        error "Failed to checkout code from ${url} on branch ${branch}. Error: ${e.message}"
    }
}
