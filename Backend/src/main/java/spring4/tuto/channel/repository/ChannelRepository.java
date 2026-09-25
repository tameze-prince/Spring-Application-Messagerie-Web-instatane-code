package spring4.tuto.channel.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import spring4.tuto.channel.domain.Channel;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChannelRepository extends JpaRepository<Channel, UUID> {
    Optional<Channel> findByConversation_Username(String username);
    boolean existsByConversation_Username(String username);
}
