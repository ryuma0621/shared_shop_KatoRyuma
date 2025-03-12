package jp.co.sss.shop.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jp.co.sss.shop.entity.Item;

/**
 * itemsテーブル用リポジトリ
 *
 * @author System Shared
 */
@Repository
public interface ItemRepository extends JpaRepository<Item, Integer> {

	/**  商品情報をすべて取得
	 * @return 商品エンティティのリスト
	 */
	@Query("SELECT new Item(i.id, i.name, i.price,i.description, i.image, c.name) FROM Item i INNER JOIN i.category c INNER JOIN i.orderItemList oi WHERE i.deleteFlag = 0 GROUP BY i.id, i.name, i.price,i.description, i.image, c.name ORDER BY COUNT(i.id) DESC,i.id ASC")
	public List<Item> findAllOrderById();

	/**  商品情報を新着順で検索
	 * @param deleteFlag 削除フラグ
	 * @return 商品エンティティのリスト
	 */
	public List<Item> findByDeleteFlagOrderByInsertDateDescIdDesc(int deleteFlag);

	/**
	 * 商品情報を登録日付順に取得
	 * @param deleteFlag 削除フラグ
	 * @param pageable ページング情報
	 * @return 商品エンティティのページオブジェクト
	 */
	@Query("SELECT i FROM Item i INNER JOIN i.category c WHERE i.deleteFlag =:deleteFlag ORDER BY i.insertDate DESC,i.id DESC")
	Page<Item> findByDeleteFlagOrderByInsertDateDescPage(
			@Param(value = "deleteFlag") int deleteFlag, Pageable pageable);

	/** 商品情報をカテゴリIDで条件検索(新着順
	 * @param categoryId カテゴリID
	 * @param deleteFlag 削除フラグ
	 * @return 商品エンティティのリスト
	 */
	public List<Item> findByCategoryIdAndDeleteFlagOrderByInsertDateDescIdDesc(int categoryId, int deleteFlag);

	/**  商品情報をカテゴリIDで条件検索(売れ筋順)
	 * @param categoryId カテゴリID
	 * @return 商品エンティティのリスト
	 */
	@Query("SELECT new Item(i.id, i.name, i.price, i.description, i.image, c.name) FROM Item i INNER JOIN i.category c INNER JOIN i.orderItemList oi WHERE i.deleteFlag = 0 AND c.id = :categoryId GROUP BY i.id, i.name, i.price, i.description, i.image, c.name ORDER BY COUNT(i.id) DESC,i.id ASC")
	public List<Item> findByCategoryIdOrderById(@Param("categoryId") int categoryId);

	/**  注文数より在庫数の少ない商品情報を検索
	 * @param id 商品ID
	 * @param orderNum 注文数
	 * @return 商品エンティティ
	 */
	//TODO ここにメソッドを定義;

	/**
	 * 商品IDと削除フラグを条件に検索
	 * @param id 商品ID
	 * @param deleteFlag 削除フラグ
	 * @return 商品エンティティ
	 */
	public Item findByIdAndDeleteFlag(Integer id, int deleteFlag);

	/**
	 * 商品名と削除フラグを条件に検索
	 * @param name 商品名
	 * @param notDeleted 削除フラグ
	 * @return 商品エンティティ
	 */
	public Item findByNameAndDeleteFlag(String name, int notDeleted);
}
