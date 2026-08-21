// AgroLink seed data
// 20 nodes: 3 Farms, 5 Livestock, 2 FishPonds, 4 Feeds, 3 Suppliers, 3 Diseases

CREATE CONSTRAINT farm_id IF NOT EXISTS
FOR (n:Farm) REQUIRE n.id IS UNIQUE;

CREATE CONSTRAINT livestock_id IF NOT EXISTS
FOR (n:Livestock) REQUIRE n.id IS UNIQUE;

CREATE CONSTRAINT pond_id IF NOT EXISTS
FOR (n:FishPond) REQUIRE n.id IS UNIQUE;

CREATE CONSTRAINT feed_id IF NOT EXISTS
FOR (n:Feed) REQUIRE n.id IS UNIQUE;

CREATE CONSTRAINT supplier_id IF NOT EXISTS
FOR (n:Supplier) REQUIRE n.id IS UNIQUE;

CREATE CONSTRAINT disease_id IF NOT EXISTS
FOR (n:Disease) REQUIRE n.id IS UNIQUE;

// Farms
MERGE (f1:Farm {id: 'F001'})
SET f1.name = 'Farm Alpha', f1.location = 'Lagos', f1.type = 'Mixed';

MERGE (f2:Farm {id: 'F002'})
SET f2.name = 'Farm Beta', f2.location = 'Ogun', f2.type = 'Poultry';

MERGE (f3:Farm {id: 'F003'})
SET f3.name = 'Farm Delta', f3.location = 'Oyo', f3.type = 'Mixed';

// Livestock
MERGE (l1:Livestock {id: 'L001'})
SET l1.type = 'Broiler', l1.quantity = 500, l1.status = 'Healthy';

MERGE (l2:Livestock {id: 'L002'})
SET l2.type = 'Layers', l2.quantity = 300, l2.status = 'At Risk';

MERGE (l3:Livestock {id: 'L003'})
SET l3.type = 'Broiler', l3.quantity = 700, l3.status = 'Healthy';

MERGE (l4:Livestock {id: 'L004'})
SET l4.type = 'Broiler', l4.quantity = 600, l4.status = 'Healthy';

MERGE (l5:Livestock {id: 'L005'})
SET l5.type = 'Layers', l5.quantity = 450, l5.status = 'Monitoring';

// Fish ponds
MERGE (p1:FishPond {id: 'P001'})
SET p1.name = 'Alpha Main Pond', p1.fishType = 'Catfish', p1.quantity = 1500, p1.status = 'Healthy';

MERGE (p2:FishPond {id: 'P002'})
SET p2.name = 'Delta Grow-Out Pond', p2.fishType = 'Tilapia', p2.quantity = 1000, p2.status = 'Healthy';

// Feeds
MERGE (fe1:Feed {id: 'FE001'})
SET fe1.name = 'Layer Pro', fe1.category = 'Poultry';

MERGE (fe2:Feed {id: 'FE002'})
SET fe2.name = 'Broiler Max', fe2.category = 'Poultry';

MERGE (fe3:Feed {id: 'FE003'})
SET fe3.name = 'Fish Grower', fe3.category = 'Aquaculture';

MERGE (fe4:Feed {id: 'FE004'})
SET fe4.name = 'Aqua Premium', fe4.category = 'Aquaculture';

// Suppliers
MERGE (s1:Supplier {id: 'S001'})
SET s1.name = 'GreenFeed', s1.location = 'Lagos', s1.status = 'Active';

MERGE (s2:Supplier {id: 'S002'})
SET s2.name = 'AquaFeeds', s2.location = 'Ogun', s2.status = 'Active';

MERGE (s3:Supplier {id: 'S003'})
SET s3.name = 'Prime Agro Supplies', s3.location = 'Oyo', s3.status = 'Active';

// Diseases
MERGE (d1:Disease {id: 'D001'})
SET d1.name = 'Newcastle Disease', d1.severity = 'High';

MERGE (d2:Disease {id: 'D002'})
SET d2.name = 'Avian Influenza', d2.severity = 'Critical';

MERGE (d3:Disease {id: 'D003'})
SET d3.name = 'Fowl Pox', d3.severity = 'Medium';

// Farm -> livestock
MATCH (f:Farm {id: 'F001'}), (l:Livestock {id: 'L001'}) CREATE (f)-[:HAS_LIVESTOCK]->(l);
MATCH (f:Farm {id: 'F001'}), (l:Livestock {id: 'L004'}) CREATE (f)-[:HAS_LIVESTOCK]->(l);
MATCH (f:Farm {id: 'F002'}), (l:Livestock {id: 'L002'}) CREATE (f)-[:HAS_LIVESTOCK]->(l);
MATCH (f:Farm {id: 'F003'}), (l:Livestock {id: 'L003'}) CREATE (f)-[:HAS_LIVESTOCK]->(l);
MATCH (f:Farm {id: 'F003'}), (l:Livestock {id: 'L005'}) CREATE (f)-[:HAS_LIVESTOCK]->(l);

// Farm -> ponds
MATCH (f:Farm {id: 'F001'}), (p:FishPond {id: 'P001'}) CREATE (f)-[:HAS_POND]->(p);
MATCH (f:Farm {id: 'F003'}), (p:FishPond {id: 'P002'}) CREATE (f)-[:HAS_POND]->(p);

// Livestock -> feed
MATCH (l:Livestock {id: 'L001'}), (fe:Feed {id: 'FE002'}) CREATE (l)-[:CONSUMES]->(fe);
MATCH (l:Livestock {id: 'L002'}), (fe:Feed {id: 'FE001'}) CREATE (l)-[:CONSUMES]->(fe);
MATCH (l:Livestock {id: 'L003'}), (fe:Feed {id: 'FE002'}) CREATE (l)-[:CONSUMES]->(fe);
MATCH (l:Livestock {id: 'L004'}), (fe:Feed {id: 'FE002'}) CREATE (l)-[:CONSUMES]->(fe);
MATCH (l:Livestock {id: 'L005'}), (fe:Feed {id: 'FE001'}) CREATE (l)-[:CONSUMES]->(fe);

// Pond -> feed
MATCH (p:FishPond {id: 'P001'}), (fe:Feed {id: 'FE003'}) CREATE (p)-[:CONSUMES]->(fe);
MATCH (p:FishPond {id: 'P002'}), (fe:Feed {id: 'FE003'}) CREATE (p)-[:CONSUMES]->(fe);
MATCH (p:FishPond {id: 'P002'}), (fe:Feed {id: 'FE004'}) CREATE (p)-[:CONSUMES]->(fe);

// Supplier -> feed
MATCH (s:Supplier {id: 'S001'}), (fe:Feed {id: 'FE001'}) CREATE (s)-[:SUPPLIES]->(fe);
MATCH (s:Supplier {id: 'S001'}), (fe:Feed {id: 'FE002'}) CREATE (s)-[:SUPPLIES]->(fe);
MATCH (s:Supplier {id: 'S002'}), (fe:Feed {id: 'FE003'}) CREATE (s)-[:SUPPLIES]->(fe);
MATCH (s:Supplier {id: 'S002'}), (fe:Feed {id: 'FE004'}) CREATE (s)-[:SUPPLIES]->(fe);
MATCH (s:Supplier {id: 'S003'}), (fe:Feed {id: 'FE002'}) CREATE (s)-[:SUPPLIES]->(fe);
MATCH (s:Supplier {id: 'S003'}), (fe:Feed {id: 'FE004'}) CREATE (s)-[:SUPPLIES]->(fe);

// Disease -> livestock
MATCH (d:Disease {id: 'D001'}), (l:Livestock {id: 'L002'}) MERGE (d)-[:AFFECTS]->(l);
MATCH (d:Disease {id: 'D001'}), (l:Livestock {id: 'L005'}) MERGE (d)-[:AFFECTS]->(l);
MATCH (d:Disease {id: 'D002'}), (l:Livestock {id: 'L001'}) MERGE (d)-[:AFFECTS]->(l);
MATCH (d:Disease {id: 'D002'}), (l:Livestock {id: 'L003'}) MERGE (d)-[:AFFECTS]->(l);
MATCH (d:Disease {id: 'D003'}), (l:Livestock {id: 'L002'}) MERGE (d)-[:AFFECTS]->(l);
MATCH (d:Disease {id: 'D003'}), (l:Livestock {id: 'L004'}) MERGE (d)-[:AFFECTS]->(l);
