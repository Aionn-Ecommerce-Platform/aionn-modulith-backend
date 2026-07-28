package com.aionn.chat.application.service;

import com.aionn.chat.application.dto.block.command.BlockCommands;
import com.aionn.chat.application.port.out.UserBlockPersistencePort;
import com.aionn.chat.domain.exception.ChatErrorCode;
import com.aionn.chat.domain.exception.ChatException;
import com.aionn.chat.domain.model.UserBlock;
import com.aionn.sharedkernel.application.port.EventPublisher;
import com.aionn.sharedkernel.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserBlockService {

    private final UserBlockPersistencePort repository;
    private final EventPublisher eventPublisher;
    private final Clock clock;

    public UserBlock block(BlockCommands.BlockUser command) {
        Optional<UserBlock> existing = repository.findActive(command.blockerId(), command.blockedId());
        if (existing.isPresent()) {
            return existing.get();
        }
        UserBlock block = UserBlock.block(IdGenerator.ulid(),
                command.blockerId(), command.blockedId(), command.reason(), clock);
        UserBlock saved = repository.save(block);
        eventPublisher.publish(block.pullEvents());
        return saved;
    }

    public UserBlock unblock(BlockCommands.UnblockUser command) {
        UserBlock block = repository.findActive(command.blockerId(), command.blockedId())
                .orElseThrow(() -> new ChatException(ChatErrorCode.BLOCK_NOT_FOUND));
        block.unblock(clock);
        UserBlock saved = repository.save(block);
        eventPublisher.publish(block.pullEvents());
        return saved;
    }

    @Transactional(readOnly = true)
    public List<UserBlock> listMyBlocks(String userId) {
        return repository.findByBlocker(userId);
    }
}
