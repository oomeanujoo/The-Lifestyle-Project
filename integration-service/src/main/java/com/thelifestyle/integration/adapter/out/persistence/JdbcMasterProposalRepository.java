package com.thelifestyle.integration.adapter.out.persistence;

import com.thelifestyle.integration.application.port.out.MasterProposalRepository;
import com.thelifestyle.integration.domain.MasterProposal;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcMasterProposalRepository implements MasterProposalRepository {
    private static final String SELECT_COLUMNS =
        "id, master_type, proposed_data::text AS proposed_data, provenance, verification_status, acceptance_status, created_at, accepted_at";

    private final JdbcTemplate jdbcTemplate;

    public JdbcMasterProposalRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public MasterProposal create(String masterType, String proposedDataJson, String provenance) {
        return jdbcTemplate.queryForObject("""
            INSERT INTO integration.master_proposal (master_type, proposed_data, provenance)
            VALUES (?, ?::jsonb, ?)
            RETURNING %s
            """.formatted(SELECT_COLUMNS), JdbcMasterProposalRepository::mapRow, masterType, proposedDataJson, provenance);
    }

    @Override
    public MasterProposal create(String masterType, String proposedDataJson, String provenance, String verificationStatus) {
        return jdbcTemplate.queryForObject("""
            INSERT INTO integration.master_proposal (master_type, proposed_data, provenance, verification_status)
            VALUES (?, ?::jsonb, ?, ?)
            RETURNING %s
            """.formatted(SELECT_COLUMNS), JdbcMasterProposalRepository::mapRow, masterType, proposedDataJson, provenance, verificationStatus);
    }

    @Override
    public List<MasterProposal> findByStatus(String acceptanceStatus) {
        return jdbcTemplate.query("""
            SELECT %s FROM integration.master_proposal WHERE acceptance_status = ? ORDER BY created_at DESC
            """.formatted(SELECT_COLUMNS), JdbcMasterProposalRepository::mapRow, acceptanceStatus);
    }

    @Override
    public List<MasterProposal> findByMasterType(String masterType) {
        return jdbcTemplate.query("""
            SELECT %s FROM integration.master_proposal WHERE master_type = ? ORDER BY created_at DESC
            """.formatted(SELECT_COLUMNS), JdbcMasterProposalRepository::mapRow, masterType);
    }

    @Override
    public Optional<MasterProposal> findById(UUID id) {
        return jdbcTemplate.query("""
            SELECT %s FROM integration.master_proposal WHERE id = ?
            """.formatted(SELECT_COLUMNS), JdbcMasterProposalRepository::mapRow, id).stream().findFirst();
    }

    // Only ever transitions a row currently DRAFT — an already-accepted or
    // already-rejected proposal is a closed decision, never silently
    // re-flipped by a second call.
    @Override
    public Optional<MasterProposal> accept(UUID id) {
        return jdbcTemplate.query("""
            UPDATE integration.master_proposal SET acceptance_status = 'ACCEPTED', accepted_at = now()
            WHERE id = ? AND acceptance_status = 'DRAFT'
            RETURNING %s
            """.formatted(SELECT_COLUMNS), JdbcMasterProposalRepository::mapRow, id).stream().findFirst();
    }

    @Override
    public Optional<MasterProposal> reject(UUID id) {
        return jdbcTemplate.query("""
            UPDATE integration.master_proposal SET acceptance_status = 'REJECTED'
            WHERE id = ? AND acceptance_status = 'DRAFT'
            RETURNING %s
            """.formatted(SELECT_COLUMNS), JdbcMasterProposalRepository::mapRow, id).stream().findFirst();
    }

    private static MasterProposal mapRow(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        var acceptedAt = rs.getTimestamp("accepted_at");
        return new MasterProposal(
            (UUID) rs.getObject("id"),
            rs.getString("master_type"),
            rs.getString("proposed_data"),
            rs.getString("provenance"),
            rs.getString("verification_status"),
            rs.getString("acceptance_status"),
            rs.getTimestamp("created_at").toInstant().toString(),
            acceptedAt == null ? null : acceptedAt.toInstant().toString());
    }
}
