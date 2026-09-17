-- V2: 잔재 테이블 정리.
-- daily_box_office 는 커밋 6cc4e4c 에서 박스오피스 저장을 DB 에서 Spring Cache 로 전환하며 엔티티가 사라졌지만 운영 테이블은 남아 있었다.
-- 운영에만 존재하므로(V1 에 없음) 로컬·테스트·새 설치에서는 no-op 이 되도록 IF EXISTS 를 붙인다.
DROP TABLE IF EXISTS daily_box_office;
