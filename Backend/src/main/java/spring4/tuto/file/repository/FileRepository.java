package spring4.tuto.file.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import spring4.tuto.file.domain.FileEntity;

import java.util.List;
import java.util.UUID;

@Repository
public interface FileRepository extends JpaRepository<FileEntity, UUID> {
    List<FileEntity> findByOwnerId(UUID ownerId);
}
