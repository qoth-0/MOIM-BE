package com.team1.moim.domain.chat.repository;

import com.team1.moim.domain.chat.entity.Room;
import com.team1.moim.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {
    List<Room> findByMember (Member member);

}
