-- V3__seed_categories_and_products.sql
-- Sample D-Mart product catalogue

INSERT IGNORE INTO categories (name, slug, description, active) VALUES
('Grocery & Staples',  'grocery-staples',    'Rice, Atta, Dal, Oil and daily essentials', TRUE),
('Fruits & Vegetables','fruits-vegetables',  'Fresh farm produce delivered daily',          TRUE),
('Dairy & Breakfast',  'dairy-breakfast',    'Milk, Butter, Eggs, Cereals',                TRUE),
('Snacks & Beverages', 'snacks-beverages',   'Chips, Biscuits, Juices, Cold Drinks',       TRUE),
('Personal Care',      'personal-care',      'Soaps, Shampoos, Toothpaste',                TRUE),
('Home & Kitchen',     'home-kitchen',       'Utensils, Cleaning, Storage',                TRUE),
('Electronics',        'electronics',        'Small appliances and accessories',            TRUE);

INSERT IGNORE INTO products
  (sku, name, description, category_id, price, discount_percent, stock_quantity, active)
SELECT
  v.sku, v.name, v.description, c.id, v.price, v.discount_percent, v.stock_quantity, TRUE
FROM (VALUES
  ROW('SKU-GRC-001','Basmati Rice 5 kg',       'Premium long-grain basmati rice',         'grocery-staples',    299.00,  5.00, 200),
  ROW('SKU-GRC-002','Tata Salt 1 kg',           'Free-flow iodised salt',                  'grocery-staples',     20.00,  0.00, 500),
  ROW('SKU-GRC-003','Aashirvaad Atta 5 kg',     'Whole wheat flour',                       'grocery-staples',    230.00,  3.00, 150),
  ROW('SKU-GRC-004','Fortune Sunflower Oil 1 L', 'Refined sunflower oil',                  'grocery-staples',    145.00,  0.00, 300),
  ROW('SKU-GRC-005','Toor Dal 1 kg',            'Premium toor dal',                        'grocery-staples',    120.00,  0.00, 250),
  ROW('SKU-DAI-001','Amul Milk 1 L',            'Full-cream pasteurised milk',             'dairy-breakfast',     68.00,  0.00, 100),
  ROW('SKU-DAI-002','Amul Butter 500 g',        'Pasteurised table butter',               'dairy-breakfast',    265.00,  0.00, 80),
  ROW('SKU-DAI-003','Britannia Eggs 12 pcs',    'Farm-fresh eggs',                         'dairy-breakfast',     96.00,  0.00, 60),
  ROW('SKU-DAI-004','Kellogg Corn Flakes 500 g','Classic breakfast cereal',               'dairy-breakfast',    225.00, 10.00, 90),
  ROW('SKU-SNK-001','Lay''s Classic Salted',    'Potato chips 50 g',                      'snacks-beverages',    20.00,  0.00, 400),
  ROW('SKU-SNK-002','Parle-G Biscuits 800 g',  'Glucose biscuits',                        'snacks-beverages',    55.00,  0.00, 350),
  ROW('SKU-SNK-003','Tropicana Orange 1 L',     'Real fruit juice',                        'snacks-beverages',    99.00,  5.00, 180),
  ROW('SKU-SNK-004','Coca-Cola 2 L',            'Chilled soft drink',                      'snacks-beverages',    95.00,  0.00, 220),
  ROW('SKU-PER-001','Dove Soap 3×100 g',        'Moisturising beauty soap',               'personal-care',      145.00,  8.00, 200),
  ROW('SKU-PER-002','Head & Shoulders 400 ml',  'Anti-dandruff shampoo',                  'personal-care',      299.00, 12.00, 120),
  ROW('SKU-PER-003','Colgate MaxFresh 150 g',   'Cooling crystal toothpaste',             'personal-care',       99.00,  5.00, 300),
  ROW('SKU-HOM-001','Vim Dishwash Liquid 500 ml','Lemon-fresh dishwashing liquid',        'home-kitchen',        89.00,  0.00, 250),
  ROW('SKU-HOM-002','Lizol Disinfectant 500 ml','Multi-surface disinfectant',             'home-kitchen',       145.00,  7.00, 180),
  ROW('SKU-ELC-001','Philips LED Bulb 9W',      'Energy-saving LED bulb warm white',      'electronics',         85.00,  0.00, 150),
  ROW('SKU-ELC-002','Syska Power Strip 4 Port', '4-socket surge-protected power strip',   'electronics',        399.00, 15.00, 75)
) AS v(sku, name, description, cat_slug, price, discount_percent, stock_quantity)
JOIN categories c ON c.slug = v.cat_slug;
