package study.querydsl.repository;

import com.querydsl.core.QueryResults;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;
import org.springframework.data.support.PageableExecutionUtils;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.dto.QMemberTeamDto;
import study.querydsl.entity.Member;

import javax.persistence.EntityManager;
import java.util.List;

import static org.springframework.util.StringUtils.hasText;
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

//MemberRepository에 적용할 것이기 떄문에 이름을 꼭 MemberRepositoryImpl로 지어야한다. MemberRepositoryCustom은 아무거나 상관없음.
//Querydsl 기능의 손쉬운 사용을 위해 QuerydslRepositorySupport 를 extends 함.
public class MemberRepositoryImpl extends QuerydslRepositorySupport implements MemberRepositoryCustom{
//public class MemberRepositoryImpl implements MemberRepositoryCustom{

//    QuerydslRepositorySupport 상속으로 주석처리.
    private final JPAQueryFactory queryFactory;

//    todo. 오류 발생할 경우 EntityManger em 으로 주입.
    public MemberRepositoryImpl() {
        super(Member.class);
        this.queryFactory = new JPAQueryFactory(getEntityManager());
    }

//    QuerydslRepositorySupport 상속. QuerydslRepositorySupport 는 추상클래스이므로 init.
//    public MemberRepositoryImpl() {
//        super(Member.class);
//    }

    @Override
    public List<MemberTeamDto> searchByWhereParam(MemberSearchCondition condition) {
//        QuerydslRepositorySupport 상속 활용
//        List<MemberTeamDto> result = from(member) // querydsl은 3버전에서는 select가 아닌 from부터 시작했다, QuerydslRepositorySupport 가 3버전에 만들어져서 select가 뒤에 오고 from이 먼저온다.
//                .leftJoin(member.team, team)
//                .where(
//                        usernameEq(condition.getUsername()),
//                        teamNameEq(condition.getTeamName()),
//                        ageGoe(condition.getAgeGoe()),
//                        ageLoe(condition.getAgeLoe())
//                )
//                .select(new QMemberTeamDto(
//                        member.id.as("memberId"),
//                        member.username,
//                        member.age,
//                        member.id.as("teamId"),
//                        team.name.as("teamName")
//                ))
//                .fetch();


//        QuerydslRepositorySupport 상속으로 인해 queryFactory 주석처리.
        return queryFactory
                .select(new QMemberTeamDto(
                        member.id.as("memberId"),
                        member.username,
                        member.age,
                        member.id.as("teamId"),
                        team.name.as("teamName")
                ))
                .from(member)
                .leftJoin(member.team, team)
                .where(
                        usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe())
                )
                .fetch();
    }

    @Override
    public Page<MemberTeamDto> searchPageSimple(MemberSearchCondition condition, Pageable pageable) {
        QueryResults<MemberTeamDto> results = queryFactory
                .select(new QMemberTeamDto(
                        member.id.as("memberId"),
                        member.username,
                        member.age,
                        member.id.as("teamId"),
                        team.name.as("teamName")
                ))
                .from(member)
                .leftJoin(member.team, team)
                .where(
                        usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe())
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetchResults();

//        fetchResults() 는 count 쿼리 하나, content query 하나 총 두개의 쿼리 발행
        List<MemberTeamDto> content = results.getResults();
        long total = results.getTotal();

        return new PageImpl<>(content, pageable, total);
    }

//    QuerydslRepositorySupport 활용하여 페이징 간편화
    public Page<MemberTeamDto> searchPageSimpleWithQuerydslRepositorySupport(MemberSearchCondition condition, Pageable pageable) {
        JPQLQuery<MemberTeamDto> jpqlQuery = from(member)
                .leftJoin(member.team, team)
                .where(
                        usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe())
                )
                .select(new QMemberTeamDto(
                        member.id.as("memberId"),
                        member.username,
                        member.age,
                        member.id.as("teamId"),
                        team.name.as("teamName")
                ));

//        QuerydslRepositorySupport 활용하여 페이징 간편화
        JPQLQuery<MemberTeamDto> query = getQuerydsl().applyPagination(pageable, jpqlQuery);
        List<MemberTeamDto> results = query.fetch();

//        단점.
//        1. sort가 동작하지않음(버그). -> 직접 처리해야한다.
//        2. select로 시작할 수 없다.
        return new PageImpl<>(results, pageable, results.size());
    }

    @Override
    public Page<MemberTeamDto> searchPageComplex(MemberSearchCondition condition, Pageable pageable) {
//        content용 쿼리
        List<MemberTeamDto> content = queryFactory
                .select(new QMemberTeamDto(
                        member.id.as("memberId"),
                        member.username,
                        member.age,
                        member.id.as("teamId"),
                        team.name.as("teamName")
                ))
                .from(member)
                .leftJoin(member.team, team)
                .where(
                        usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe())
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

//        count 용 쿼리
        long total = queryFactory.select(member)
                .from(member)
                .leftJoin(member.team, team)
                .where(
                        usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe())
                )
                .fetchCount();
//        count 쿼리를 분리하는 이유
//        content 쿼리는 복잡한데 count query는 join이 필요없다거나 하는 이유로 단순해질 수 있음.
//        count 쿼리는 이런 경우 분리하여 성능 개선이 가능.
//        또한 content 쿼리를 먼저 실행 하고 content의 내용이 없으면 count query는 발행하지 않거나,
//        반대로 count 쿼리를 먼저 실행하고 count > 0 이어야 content query를 발행하는 등의 성능 개선이 가능하다

        return new PageImpl<>(content, pageable, total);
    }

//    count query 생략 가능한 경우
//    1. 페이지 시작이면서 컨텐츠 사이즈가 페이지 사이즈보다 작을 경우
//    2, 페이지 마지막일때. (offset + 컨텐츠 사이즈를 더해서 전체 사이즈를 구함)
    @Override
    public Page<MemberTeamDto> searchPageCountOptimization(MemberSearchCondition condition, Pageable pageable) {
    //        content용 쿼리
        List<MemberTeamDto> content = queryFactory
                .select(new QMemberTeamDto(
                        member.id.as("memberId"),
                        member.username,
                        member.age,
                        member.id.as("teamId"),
                        team.name.as("teamName")
                ))
                .from(member)
                .leftJoin(member.team, team)
                .where(
                        usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe())
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

    //        count 용 쿼리
        JPAQuery<Member> countQuery = queryFactory.select(member)
                .from(member)
                .leftJoin(member.team, team)
                .where(
                        usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe())
                );

//        countQuery는 아직 쿼리 실행 전. countQuery.fetchCount() 해야 쿼리가 날아간다.
//        PageableExecutionUtils.getPage 는 content 사이즈와 page사이즈를 비교하고, 이때 첫 페이지거나 마지막 페이지면
//        countQuery.fetchCount() 를 호출하지 않아 count 쿼리 발행하지 않음으로써 쿼리 최적화가 가능하다.
//        들어가보면 특정 조건일때만 return new PageImpl<>(content, pageable, totalSupplier.getAsLong()) 을 호출하는 것을 확인할 수 있다.
        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchCount);
//        return PageableExecutionUtils.getPage(content, pageable, () -> countQuery.fetchCount()); //위 아래 두개 같은 표현
    }

    private BooleanExpression usernameEq(String username) {
        return hasText(username) ? member.username.eq(username) : null;
    }

    private BooleanExpression teamNameEq(String teamName) {
        return hasText(teamName) ? team.name.eq(teamName) : null;
    }

    private BooleanExpression ageGoe(Integer ageGoe) {
        return ageGoe != null ? member.age.goe(ageGoe) : null;
    }

    private BooleanExpression ageLoe(Integer ageLoe) {
        return ageLoe != null ? member.age.loe(ageLoe) : null;
    }
}
