package com.team1.moim.domain.chat.repository;

import com.team1.moim.domain.chat.entity.MemberRoom;
import com.team1.moim.domain.chat.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MemberRoomRepository extends JpaRepository<MemberRoom, Long> {

    // 주어진 roomId에 대해 isHost 속성이 true인 MemberRoom을 찾는다.
    // 반환한 MemberRoom 객체를 통해서 호스트 정보를 얻을 수 있다.
//    @Query("SELECT mr FROM MemberRoom mr WHERE mr.room.id = :roomId AND mr.isHost = true")
//    MemberRoom findHostByRoomId(@Param("roomId") Long roomId);

    List<MemberRoom> findByRoom(Room room);
}
