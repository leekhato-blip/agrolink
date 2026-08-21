// AgroLink graph verification
// Expected counts:
// - 20 nodes
// - 27 relationships

MATCH (n)
WITH count(n) AS nodes
MATCH ()-[r]->()
RETURN nodes, count(r) AS relationships;
