CMD="docker-compose"
if ! command -v docker-compose &> /dev/null
then
    echo "docker-compose not found, let's try 'docker compose'"
    CMD="docker compose"
fi

$CMD up -d

echo "--------------------------------------------"
echo "Urls"
echo "--------------------------------------------"

echo "Yaci Viewer                   : http://localhost:5173"
echo "Yaci Store Swagger UI         : http://localhost:8080/swagger-ui.html"

echo ""
echo "Yaci Store Api URL            : http://localhost:8080/api/v1/"
echo ""
echo "Pool Id                       : pool1wvqhvyrgwch4jq9aa84hc8q4kzvyq2z3xr6mpafkqmx9wce39zy"
echo ""

echo "--------------------------------------------"
echo "Other Urls"
echo "--------------------------------------------"

echo "Ogmios Url (Optional)         : ws://localhost:1337"
echo "Kupo Url   (Optional)         : http://localhost:1442"

echo ""
echo "--------------------------------------------"
echo "Node Ports"
echo "--------------------------------------------"

echo "n2n port                      : localhost:3001"
echo "n2c port for remote client    : localhost:3333"
echo ""

