package spring4.tuto.group.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import spring4.tuto.group.domain.Group;

import java.util.UUID;

@Repository
public interface GroupRepository extends JpaRepository<Group, UUID> {
}
