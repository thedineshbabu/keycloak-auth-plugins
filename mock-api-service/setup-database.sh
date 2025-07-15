#!/bin/bash

# Database Setup Script for SAML Metadata API
# This script helps set up the PostgreSQL database for the mock API service

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
DB_NAME="ext_api"
DB_USER="opal_user"
DB_PASSWORD="opal_password"
DB_HOST="localhost"
DB_PORT="5432"

echo -e "${BLUE}🚀 SAML Metadata API Database Setup${NC}"
echo "=================================="

# Function to print colored output
print_status() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

# Check if PostgreSQL is running
echo "Checking PostgreSQL service..."
if ! pg_isready -h $DB_HOST -p $DB_PORT > /dev/null 2>&1; then
    print_error "PostgreSQL is not running on $DB_HOST:$DB_PORT"
    echo "Please start PostgreSQL and try again."
    exit 1
fi
print_status "PostgreSQL is running"

# Check if database exists
echo "Checking if database exists..."
if psql -h $DB_HOST -p $DB_PORT -U postgres -lqt | cut -d \| -f 1 | grep -qw $DB_NAME; then
    print_warning "Database '$DB_NAME' already exists"
    read -p "Do you want to recreate it? (y/N): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        echo "Dropping existing database..."
        dropdb -h $DB_HOST -p $DB_PORT -U postgres $DB_NAME 2>/dev/null || true
        print_status "Database dropped"
    else
        print_warning "Using existing database"
    fi
else
    print_status "Database '$DB_NAME' does not exist, will create it"
fi

# Create database if it doesn't exist
if ! psql -h $DB_HOST -p $DB_PORT -U postgres -lqt | cut -d \| -f 1 | grep -qw $DB_NAME; then
    echo "Creating database '$DB_NAME'..."
    createdb -h $DB_HOST -p $DB_PORT -U postgres $DB_NAME
    print_status "Database created"
fi

# Check if user exists
echo "Checking if user exists..."
if psql -h $DB_HOST -p $DB_PORT -U postgres -t -c "SELECT 1 FROM pg_roles WHERE rolname='$DB_USER'" | grep -q 1; then
    print_warning "User '$DB_USER' already exists"
    read -p "Do you want to recreate the user? (y/N): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        echo "Dropping existing user..."
        psql -h $DB_HOST -p $DB_PORT -U postgres -c "DROP USER IF EXISTS $DB_USER;" 2>/dev/null || true
        print_status "User dropped"
    else
        print_warning "Using existing user"
    fi
else
    print_status "User '$DB_USER' does not exist, will create it"
fi

# Create user if it doesn't exist
if ! psql -h $DB_HOST -p $DB_PORT -U postgres -t -c "SELECT 1 FROM pg_roles WHERE rolname='$DB_USER'" | grep -q 1; then
    echo "Creating user '$DB_USER'..."
    psql -h $DB_HOST -p $DB_PORT -U postgres -c "CREATE USER $DB_USER WITH PASSWORD '$DB_PASSWORD';"
    print_status "User created"
fi

# Grant privileges
echo "Granting privileges..."
psql -h $DB_HOST -p $DB_PORT -U postgres -c "GRANT ALL PRIVILEGES ON DATABASE $DB_NAME TO $DB_USER;"
psql -h $DB_HOST -p $DB_PORT -U postgres -c "GRANT CONNECT ON DATABASE $DB_NAME TO $DB_USER;"
print_status "Privileges granted"

# Run the database setup script
echo "Running database setup script..."
if [ -f "database-setup.sql" ]; then
    psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -f database-setup.sql
    print_status "Database setup script executed"
else
    print_error "database-setup.sql not found"
    exit 1
fi

# Test the connection
echo "Testing database connection..."
if psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -c "SELECT COUNT(*) FROM saml_metadata;" > /dev/null 2>&1; then
    print_status "Database connection test successful"
else
    print_error "Database connection test failed"
    exit 1
fi

# Show the results
echo "Verifying setup..."
RESULT=$(psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -t -c "SELECT 'Database setup completed successfully' as status, COUNT(*) as total_records, COUNT(CASE WHEN enabled = true THEN 1 END) as enabled_records FROM saml_metadata;")

echo -e "${BLUE}Setup Results:${NC}"
echo "$RESULT" | while IFS='|' read -r status total enabled; do
    echo "  Status: $status"
    echo "  Total records: $total"
    echo "  Enabled records: $enabled"
done

echo ""
print_status "Database setup completed successfully!"
echo ""
echo -e "${BLUE}Next steps:${NC}"
echo "1. Copy env.example to .env and update database settings if needed"
echo "2. Run 'npm install' to install dependencies"
echo "3. Run 'npm start' to start the API service"
echo "4. Test the API with 'node test-saml-api.js'"
echo ""
echo -e "${BLUE}Database connection details:${NC}"
echo "  Host: $DB_HOST"
echo "  Port: $DB_PORT"
echo "  Database: $DB_NAME"
echo "  User: $DB_USER"
echo "  Password: $DB_PASSWORD" 