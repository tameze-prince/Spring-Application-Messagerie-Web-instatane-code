package spring4.tuto.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import spring4.tuto.user.domain.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    @Query(value = "SELECT * FROM users WHERE deleted_at IS NULL AND " +
            "(to_tsvector('english', coalesce(username, '') || ' ' || coalesce(first_name, '') || ' ' || coalesce(last_name, '')) @@ plainto_tsquery('english', :query) " +
            "OR LOWER(username) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(first_name) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(last_name) LIKE LOWER(CONCAT('%', :query, '%')))", nativeQuery = true)
    List<User> searchUsersFts(@Param("query") String query);
}
