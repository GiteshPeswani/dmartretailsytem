import pymysql

conn = pymysql.connect(host='localhost', user='root', password='gitesh', database='dmart_db')
cur = conn.cursor()

# Check current definition
cur.execute("SHOW COLUMNS FROM audit_events LIKE 'entity_type'")
print("audit_events current:", cur.fetchall())

cur.execute("SHOW COLUMNS FROM blockchain_transactions LIKE 'entity_type'")
print("blockchain_transactions current:", cur.fetchall())

# ALTER audit_events to include 'BATCH'
cur.execute("ALTER TABLE audit_events MODIFY COLUMN entity_type ENUM('PRODUCT', 'ORDER', 'BATCH') NOT NULL")
print("Updated audit_events entity_type enum to include 'BATCH'")

# ALTER blockchain_transactions to include 'BATCH'
cur.execute("ALTER TABLE blockchain_transactions MODIFY COLUMN entity_type ENUM('PRODUCT', 'ORDER', 'BATCH') NOT NULL")
print("Updated blockchain_transactions entity_type enum to include 'BATCH'")

conn.commit()
print("Database schema successfully updated!")
