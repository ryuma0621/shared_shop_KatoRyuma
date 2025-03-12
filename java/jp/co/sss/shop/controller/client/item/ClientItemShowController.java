package jp.co.sss.shop.controller.client.item;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import jp.co.sss.shop.bean.ItemBean;
import jp.co.sss.shop.entity.Item;
import jp.co.sss.shop.repository.ItemRepository;
import jp.co.sss.shop.service.BeanTools;
import jp.co.sss.shop.util.Constant;

/**
 * 商品管理 一覧表示機能(一般会員用)のコントローラクラス
 *
 * @author SystemShared
 */
@Controller
public class ClientItemShowController {
	/**
	 * 商品情報
	 */
	@Autowired
	ItemRepository itemRepository;

	/**
	 * Entity、Form、Bean間のデータコピーサービス
	 */
	@Autowired
	BeanTools beanTools;
	
	/**
	 * トップ画面 表示処理
	 *
	 * @param model    Viewとの値受渡し
	 * @return "index" トップ画面
	 */
	@RequestMapping(path = "/" , method = { RequestMethod.GET, RequestMethod.POST })
	public String index(Model model) {
		// 商品の並び順情報（2：売れ筋順）
		int sortType = 2;

		// 注文情報の商品情報を全件検索(売れ筋順)
		List<Item> itemList = itemRepository.findAllOrderById();
		// 注文情報が無い場合は、商品情報を検索(新着順)
		if (itemList.isEmpty()) {

			itemList = itemRepository.findByDeleteFlagOrderByInsertDateDescIdDesc(Constant.NOT_DELETED);
			if (!itemList.isEmpty()) {
				//新着商品があった場合は、並び順情報を変更
				sortType = 1;
			}
		}

		// エンティティ内の検索結果をJavaBeansにコピー
		List<ItemBean> itemBeanList = beanTools.copyEntityListToItemBeanList(itemList);

		// 商品情報をViewへ渡す
		model.addAttribute("items", itemBeanList);
		model.addAttribute("sortType", sortType);
	
		return "index";
	}

	/**
	 * 商品情報一覧カテゴリ検索 表示処理
	 *
	 * @param sortType   並べ替え条件
	 * @param categoryId カテゴリID
	 * @param model      Viewとの値受渡し
	 * @return "client/item/list" 商品情報 一覧画面へ
	 */
	@RequestMapping(path = "/client/item/list/{sortType}", method = { RequestMethod.GET, RequestMethod.POST })
	public String showItemListByCategoryId(@PathVariable Integer sortType, Integer categoryId, Model model) {

		List<Item> itemList = null;

		if (categoryId == null) {
			// カテゴリIDパラメータ無い場合、カテゴリ選択なし状態とする
			categoryId = 0;
		}

		if (categoryId == 0) {
			// カテゴリ選択無しの場合
			if (sortType == 1) {
				// 商品情報を全件検索(新着順)
				itemList = itemRepository.findByDeleteFlagOrderByInsertDateDescIdDesc(Constant.NOT_DELETED);
			} else {
				// 商品情報を全件検索(売れ筋順)
				itemList = itemRepository.findAllOrderById();
			}

		} else {
			// カテゴリ選択ありの場合
			if (sortType == 1) {
				// 商品情報をカテゴリーIDで条件検索(新着順)
				//itemList = itemRepository.findByCategoryIdOrderByInsertDate(categoryId);
				itemList = itemRepository.findByCategoryIdAndDeleteFlagOrderByInsertDateDescIdDesc(categoryId,
						Constant.NOT_DELETED);
			} else {
				// 商品情報をカテゴリーIDで条件検索(売れ筋順)
				itemList = itemRepository.findByCategoryIdOrderById(categoryId);
			}

		}

		// エンティティ内の検索結果をJavaBeansにコピー
		List<ItemBean> itemBeanList = beanTools.copyEntityListToItemBeanList(itemList);

		// 商品情報をViewへ渡す
		model.addAttribute("items", itemBeanList);
		model.addAttribute("sortType", sortType);
		model.addAttribute("categoryId", categoryId);

		return "client/item/list";
	}

	/**
	 * 詳細表示処理
	 *
	 * @param id      表示対象ID
	 * @param model   Viewとの値受渡し
	 * @return "client/item/detail" 詳細画面 表示
	 */
	@RequestMapping(path = "/client/item/detail/{id}")
	public String showItem(@PathVariable int id, Model model) {

		// 商品IDに該当する商品情報を取得する
		Item item = itemRepository.findByIdAndDeleteFlag(id, Constant.NOT_DELETED);
		if (item == null) {
			return "redirect:/syserror";
		}

		// Itemエンティティの各フィールドの値をItemBeanにコピー
		ItemBean itemBean = beanTools.copyEntityToItemBean(item);
		
		// 商品情報をViewへ渡す
		model.addAttribute("item", itemBean);

		return "client/item/detail";
	}
}
