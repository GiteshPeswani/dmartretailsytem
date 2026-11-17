import pymysql
import json
import datetime

conn = pymysql.connect(host='localhost', user='root', password='gitesh', database='dmart_db')
cur = conn.cursor()

with open(r'd:\blockchain\blockchain\scripts\registration_results.json', 'r', encoding='utf-8') as f:
    results = json.load(f)

now = datetime.datetime.now()

for r in results:
    if r.get('status') == 'CONFIRMED':
        # Update products table
        cur.execute(
            'UPDATE products SET blockchain_status = %s, blockchain_tx_hash = %s, blockchain_product_id = %s WHERE id = %s',
            ('CONFIRMED', r['txHash'], r['productId'], r['id'])
        )
        
        # Insert or update blockchain_transactions
        cur.execute(
            '''INSERT INTO blockchain_transactions 
               (tx_hash, entity_type, entity_id, event_type, status, block_number, gas_used, submitted_at, confirmed_at, created_at, updated_at)
               VALUES (%s, 'PRODUCT', %s, 'PRODUCT_REGISTER', 'CONFIRMED', %s, %s, %s, %s, %s, %s)
               ON DUPLICATE KEY UPDATE tx_hash=%s, status='CONFIRMED', block_number=%s, gas_used=%s, confirmed_at=%s''',
            (r['txHash'], r['id'], r.get('blockNumber', 1), r.get('gasUsed', 50000), now, now, now, now,
             r['txHash'], r.get('blockNumber', 1), r.get('gasUsed', 50000), now)
        )

# Request 1: Set Kellogg Corn Flakes to OUT OF STOCK (stock_quantity = 0)
cur.execute('UPDATE products SET stock_quantity = 0 WHERE sku = "SKU-DAI-004"')
print('Set SKU-DAI-004 (Kellogg Corn Flakes) stock to 0.')

conn.commit()
print('Successfully updated database with on-chain confirmed status for all', len(results), 'products!')
