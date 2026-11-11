package com.aionn.catalog.infrastructure.persistence.adapter.merchant;

import com.aionn.catalog.application.port.out.merchant.CollectionPersistencePort;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CollectionJdbcAdapter implements CollectionPersistencePort {

    private static final RowMapper<CollectionView> MAPPER = CollectionJdbcAdapter::map;

    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedJdbcTemplate;

    @Override
    public List<CollectionView> findAll(boolean activeOnly) {
        String sql = "SELECT * FROM collections" + (activeOnly ? " WHERE active = TRUE" : "")
                + " ORDER BY display_order ASC, name ASC";
        return jdbcTemplate.query(sql, MAPPER);
    }

    @Override
    public Optional<CollectionView> findByIdOrSlug(String key) {
        return jdbcTemplate.query(
                "SELECT * FROM collections WHERE collection_id = ? OR slug = ? LIMIT 1",
                MAPPER, key, key).stream().findFirst();
    }

    @Override
    public CollectionView save(CollectionView collection) {
        int updated = jdbcTemplate.update("""
                UPDATE collections
                   SET slug = ?, name = ?, description = ?, image_url = ?, active = ?,
                       display_order = ?, updated_at = ?
                 WHERE collection_id = ?
                """, collection.slug(), collection.name(), collection.description(),
                collection.imageUrl(), collection.active(), collection.displayOrder(),
                Timestamp.from(collection.updatedAt()), collection.collectionId());
        if (updated == 0) {
            jdbcTemplate.update("""
                    INSERT INTO collections
                        (collection_id, slug, name, description, image_url, active, display_order,
                         created_at, updated_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, collection.collectionId(), collection.slug(), collection.name(),
                    collection.description(), collection.imageUrl(), collection.active(),
                    collection.displayOrder(), Timestamp.from(collection.createdAt()),
                    Timestamp.from(collection.updatedAt()));
        }
        return findByIdOrSlug(collection.collectionId()).orElseThrow();
    }

    @Override
    public void delete(String collectionId) {
        jdbcTemplate.update("DELETE FROM collections WHERE collection_id = ?", collectionId);
    }

    @Override
    public long countProducts(String collectionId) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM products WHERE collection_ids ? ?",
                Long.class, collectionId);
        return count == null ? 0L : count;
    }

    @Override
    public boolean existsById(String collectionId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM collections WHERE collection_id = ?",
                Integer.class, collectionId);
        return count != null && count > 0;
    }

    @Override
    public boolean existsAll(Collection<String> collectionIds) {
        if (collectionIds == null || collectionIds.isEmpty()) {
            return true;
        }
        Long count = namedJdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM collections WHERE collection_id IN (:ids)",
                new MapSqlParameterSource("ids", collectionIds),
                Long.class);
        return count != null && count == collectionIds.size();
    }

    private static CollectionView map(ResultSet rs, int rowNum) throws SQLException {
        return new CollectionView(
                rs.getString("collection_id"),
                rs.getString("slug"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getString("image_url"),
                rs.getBoolean("active"),
                rs.getInt("display_order"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant());
    }
}
