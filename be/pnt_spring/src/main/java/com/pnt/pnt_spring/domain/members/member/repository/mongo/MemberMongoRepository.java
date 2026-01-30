package com.pnt.pnt_spring.domain.members.member.repository.mongo;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;

import com.pnt.pnt_spring.domain.members.member.entity.document.MemberDoc;

public interface MemberMongoRepository extends MongoRepository<MemberDoc, String> {
	Optional<MemberDoc> findByMemberId(Long memberId);

	@Query("{ 'memberId' : ?0 }")
	@Update("{ '$set' : { 'nickname' : ?1, 'avatarUrl' : ?2 } }")
	void updateNicknameAndAvatar(Long memberId, String nickname, String avatarUrl);

}
