package jp.co.sss.shop.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jp.co.sss.shop.entity.Order;

/**
 * ordersテーブル用リポジトリ
 *
 * @author System Shared
 */
@Repository

public interface OrderRepository extends JpaRepository<Order, Integer> {

	/**
	 * 直近に登録された注文情報の取得
	 * @return 注文情報エンティティ
	 */
	@Query("SELECT o FROM Order o ORDER BY o.insertDate DESC")
	Order findLatestOrder();

	/**
	 * 会員IDに該当する注文情報を注文日付降順で検索
	 * @param userId 会員ID
	 * @return 注文エンティティのリスト
	 */
	@EntityGraph(attributePaths = {"orderItemsList"})
	@Query("SELECT o FROM Order o WHERE o.user.id = :userId ORDER BY o.insertDate DESC")
	Page<Order> findByUserIdOrderByDateDesc(@Param("userId") Integer userId, Pageable pageable);

	/**
	 * 注文日付降順で注文情報すべてを検索
	 * @param pageable ページング情報
	 * @return 注文エンティティのページオブジェクト
	 */
	@Query("SELECT o FROM Order o ORDER BY o.insertDate DESC,o.id DESC")
	Page<Order> findAllOrderByInsertdateDescIdDesc(Pageable pageable);

}
