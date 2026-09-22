package spring4.tuto.channel.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import spring4.tuto.channel.domain.Channel;

import java.util.UUID;

@Repository
public interface ChannelRepository extends JpaRepository<Channel, UUID> {
}
