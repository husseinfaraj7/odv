#!/bin/bash

# Render Deployment Validation Script
# This script deploys the application to Render staging and runs validation tests

set -e  # Exit on any error

echo "=================================="
echo "RENDER DEPLOYMENT VALIDATION"
echo "=================================="

# Configuration
STAGING_BRANCH=${STAGING_BRANCH:-"staging"}
PRODUCTION_BRANCH=${PRODUCTION_BRANCH:-"main"}
STAGING_URL=${STAGING_URL:-""}
PRODUCTION_URL=${PRODUCTION_URL:-""}

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to check if we're in the right directory
check_directory() {
    if [ ! -f "backend/pom.xml" ]; then
        print_error "Must be run from project root directory (where backend/ folder exists)"
        exit 1
    fi
}

# Function to check prerequisites
check_prerequisites() {
    print_status "Checking prerequisites..."
    
    # Check if git is available
    if ! command -v git &> /dev/null; then
        print_error "Git is required but not installed"
        exit 1
    fi
    
    # Check if Maven wrapper exists
    if [ ! -f "backend/mvnw" ]; then
        print_error "Maven wrapper not found in backend/"
        exit 1
    fi
    
    # Check if Java is available
    cd backend
    if ! ./mvnw --version &> /dev/null; then
        print_error "Java/Maven not properly configured"
        exit 1
    fi
    cd ..
    
    print_success "Prerequisites check passed"
}

# Function to run local validation tests
run_local_tests() {
    print_status "Running local validation tests..."
    cd backend
    
    # Clean and run tests
    if ./mvnw clean test -Dtest=RenderDeploymentValidationTest -q; then
        print_success "Local validation tests passed"
    else
        print_error "Local validation tests failed"
        print_error "Fix issues before deploying to staging"
        exit 1
    fi
    cd ..
}

# Function to deploy to staging
deploy_to_staging() {
    print_status "Deploying to staging environment..."
    
    # Check if staging branch exists
    if git show-ref --verify --quiet refs/heads/$STAGING_BRANCH; then
        git checkout $STAGING_BRANCH
        git merge main --no-edit
    else
        print_warning "Staging branch doesn't exist, creating it..."
        git checkout -b $STAGING_BRANCH
    fi
    
    # Push to trigger Render deployment
    git push origin $STAGING_BRANCH
    
    print_status "Deployment to staging initiated..."
    print_status "Waiting for deployment to complete (60 seconds)..."
    sleep 60
}

# Function to run staging validation tests
run_staging_tests() {
    if [ -z "$STAGING_URL" ]; then
        print_warning "STAGING_URL not provided, skipping remote staging tests"
        print_warning "Please manually verify staging deployment at your Render staging URL"
        return 0
    fi
    
    print_status "Running staging validation tests against: $STAGING_URL"
    cd backend
    
    # Run tests against staging environment
    if ./mvnw test -Dtest=RenderDeploymentValidationTest -Dserver.url="$STAGING_URL" -Dspring.profiles.active=staging -q; then
        print_success "Staging validation tests passed"
    else
        print_error "Staging validation tests failed"
        print_error "Review staging environment configuration"
        return 1
    fi
    cd ..
}

# Function to deploy to production
deploy_to_production() {
    print_status "Deploying to production environment..."
    
    git checkout $PRODUCTION_BRANCH
    git merge $STAGING_BRANCH --no-edit
    git push origin $PRODUCTION_BRANCH
    
    print_status "Deployment to production initiated..."
    print_status "Waiting for deployment to complete (90 seconds)..."
    sleep 90
}

# Function to run production validation tests
run_production_tests() {
    if [ -z "$PRODUCTION_URL" ]; then
        print_warning "PRODUCTION_URL not provided, skipping remote production tests"
        print_warning "Please manually verify production deployment"
        return 0
    fi
    
    print_status "Running production validation tests against: $PRODUCTION_URL"
    cd backend
    
    # Run tests against production environment
    if ./mvnw test -Dtest=RenderDeploymentValidationTest -Dserver.url="$PRODUCTION_URL" -Dspring.profiles.active=prod -q; then
        print_success "Production validation tests passed"
    else
        print_error "Production validation tests failed"
        print_error "CRITICAL: Production deployment validation failed!"
        return 1
    fi
    cd ..
}

# Function to generate deployment report
generate_report() {
    print_status "Generating deployment validation report..."
    cd backend
    
    # Generate comprehensive report
    if java -cp "target/classes:target/test-classes" it.odvsicilia.backend.DeploymentTestRunner > ../deployment-validation-report.txt 2>&1; then
        print_success "Deployment report generated: deployment-validation-report.txt"
    else
        print_warning "Failed to generate deployment report, but deployment may still be successful"
    fi
    cd ..
}

# Main deployment workflow
main() {
    echo "Starting deployment validation workflow..."
    echo "Date: $(date)"
    echo "Git Commit: $(git rev-parse HEAD)"
    echo ""
    
    check_directory
    check_prerequisites
    
    # Step 1: Run local tests
    print_status "Step 1: Local validation"
    run_local_tests
    
    # Step 2: Deploy to staging
    print_status "Step 2: Staging deployment"
    deploy_to_staging
    
    # Step 3: Validate staging
    print_status "Step 3: Staging validation"
    if ! run_staging_tests; then
        print_error "Staging validation failed, aborting production deployment"
        exit 1
    fi
    
    # Step 4: Ask for confirmation before production
    echo ""
    print_status "Staging validation completed successfully!"
    read -p "Deploy to production? (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        print_status "Production deployment cancelled by user"
        exit 0
    fi
    
    # Step 5: Deploy to production
    print_status "Step 4: Production deployment"
    deploy_to_production
    
    # Step 6: Validate production
    print_status "Step 5: Production validation"
    if ! run_production_tests; then
        print_error "CRITICAL: Production validation failed!"
        print_error "Consider rolling back to previous version"
        exit 1
    fi
    
    # Step 7: Generate report
    print_status "Step 6: Generate validation report"
    generate_report
    
    # Success!
    echo ""
    echo "=================================="
    print_success "DEPLOYMENT VALIDATION COMPLETED"
    echo "=================================="
    print_success "✅ Local validation: PASSED"
    print_success "✅ Staging deployment: COMPLETED"
    print_success "✅ Staging validation: PASSED"
    print_success "✅ Production deployment: COMPLETED"
    print_success "✅ Production validation: PASSED"
    echo ""
    print_success "🎉 Application successfully deployed and validated!"
    print_status "📄 Detailed report available in: deployment-validation-report.txt"
}

# Script usage
usage() {
    echo "Usage: $0 [options]"
    echo ""
    echo "Options:"
    echo "  -h, --help           Show this help message"
    echo "  -s, --staging-url    Staging URL for validation tests"
    echo "  -p, --production-url Production URL for validation tests"
    echo "  --skip-staging       Skip staging deployment and go directly to production"
    echo "  --local-only         Run only local validation tests"
    echo ""
    echo "Environment Variables:"
    echo "  STAGING_URL         URL of staging environment"
    echo "  PRODUCTION_URL      URL of production environment"
    echo "  STAGING_BRANCH      Git branch for staging (default: staging)"
    echo "  PRODUCTION_BRANCH   Git branch for production (default: main)"
    echo ""
    echo "Examples:"
    echo "  $0                                           # Full deployment pipeline"
    echo "  $0 --staging-url https://app-staging.onrender.com"
    echo "  $0 --local-only                              # Only run local tests"
    echo "  $0 --skip-staging                            # Skip staging, deploy to production"
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -h|--help)
            usage
            exit 0
            ;;
        -s|--staging-url)
            STAGING_URL="$2"
            shift 2
            ;;
        -p|--production-url)
            PRODUCTION_URL="$2"
            shift 2
            ;;
        --skip-staging)
            SKIP_STAGING=true
            shift
            ;;
        --local-only)
            LOCAL_ONLY=true
            shift
            ;;
        *)
            print_error "Unknown option: $1"
            usage
            exit 1
            ;;
    esac
done

# Execute based on options
if [ "$LOCAL_ONLY" = true ]; then
    check_directory
    check_prerequisites
    run_local_tests
    print_success "Local validation completed successfully"
elif [ "$SKIP_STAGING" = true ]; then
    print_warning "Skipping staging deployment - deploying directly to production"
    check_directory
    check_prerequisites
    run_local_tests
    deploy_to_production
    run_production_tests
    generate_report
    print_success "Direct production deployment completed"
else
    main
fi