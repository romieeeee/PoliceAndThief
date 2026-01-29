package com.pnt.pnt_spring.domain.members.member.repository.mongo;

import com.pnt.pnt_spring.domain.members.member.entity.document.MemberDoc;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface MemberMongoRepository extends MongoRepository<MemberDoc, String> {
    Optional<MemberDoc> findByMemberId(Long memberId);
}
