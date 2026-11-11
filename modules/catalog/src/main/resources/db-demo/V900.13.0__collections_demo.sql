INSERT INTO collections (collection_id, slug, name, description, image_url, active, display_order)
VALUES
('01M74HBRD7PZVEA5VR3H1SM3N4', 'cong-nghe', 'Công nghệ', 'Điện thoại, laptop và phụ kiện công nghệ.', NULL, TRUE, 1),
('01M74HBRD8EM0XZQ6RECK4VHAR', 'thoi-trang', 'Thời trang', 'Quần áo, giày dép và phụ kiện thời trang.', NULL, TRUE, 2),
('01M74HBRD94PS4JKKC7TJ8NG9N', 'do-gia-dung', 'Đồ gia dụng', 'Nội thất, nhà cửa và đồ dùng gia đình.', NULL, TRUE, 3),
('01M74HBRDAWQ2NX5NAZQF5WVQZ', 'lam-dep', 'Làm đẹp', 'Mỹ phẩm, chăm sóc da và sức khỏe.', NULL, TRUE, 4)
ON CONFLICT (collection_id) DO NOTHING;

UPDATE products SET collection_ids = '["01M74HBRD7PZVEA5VR3H1SM3N4"]'::jsonb
 WHERE collection_ids = '[]'::jsonb
   AND category_ids ?| ARRAY['01M74HE3MQBQ0KZ9R3T2FV6DV0','01M74HE3MSF6NPJ5SQ5Q13S53B','01M74HE3MTWMV75K99ZEVDT3ZF','01M74HE3MVTZ79KB93T2ED660B'];

UPDATE products SET collection_ids = '["01M74HBRD8EM0XZQ6RECK4VHAR"]'::jsonb
 WHERE collection_ids = '[]'::jsonb
   AND category_ids ?| ARRAY['01M74HE3MWRPZD13E3VVXXS295','01M74HE3MX1K93268VV48XMWS5','01M74HE3MYEC48790V223A7HHV','01M74HE3MZTQWD9M95JYT23FJH','01M74HE3N0BWRVH8X39M1J4SNF'];

UPDATE products SET collection_ids = '["01M74HBRD94PS4JKKC7TJ8NG9N"]'::jsonb
 WHERE collection_ids = '[]'::jsonb
   AND category_ids ?| ARRAY['01M74HE3N1YMG5DHG6QA6MAH0W','01M74HE3N2C3VBKNEXH6AJGTFX','01M74HE3N3HMVFX4902GGEFF52','01M74HE3N4QS9ZJZD24JV21C72','01M74HE3N5PV0GTA9WGCR4EASP','01M74HE3N6JBESAW2BKYA16FN9'];

UPDATE products SET collection_ids = '["01M74HBRDAWQ2NX5NAZQF5WVQZ"]'::jsonb
 WHERE collection_ids = '[]'::jsonb
   AND category_ids ?| ARRAY['01M74HE3N76WFH74WV4T5K8GGG','01M74HE3N83EMA729168PYZ198'];
