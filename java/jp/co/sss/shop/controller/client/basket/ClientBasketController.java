package jp.co.sss.shop.controller.client.basket;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpSession;
import jp.co.sss.shop.bean.BasketBean;
import jp.co.sss.shop.entity.Item;
import jp.co.sss.shop.repository.ItemRepository;

/**
 * 買い物かご機能のコントローラクラス
 *
 * @author SystemShared
 */
@Controller
public class ClientBasketController {

	/** 商品情報リポジトリ */
	@Autowired
	ItemRepository itemRepository;

	/** セッション管理 */
	@Autowired
	HttpSession session;

	/**
	 * 買い物かごの一覧を表示
	 *
	 * @param model Viewとの値受渡し
	 * @return 買い物かご内の商品一覧
	 */
	@GetMapping("/client/basket/list")
	public String basketList(Model model) {
		List<BasketBean> basketList = getBasketList();
		List<String> itemNameListLessThan = new ArrayList<>();
		List<String> itemNameListZero = new ArrayList<>();

		// 在庫チェック
		for (BasketBean bean : basketList) {
			Optional<Item> optItem = itemRepository.findById(bean.getId());
			if (optItem.isPresent()) {
				Item item = optItem.get();
				if (bean.getOrderNum() > item.getStock()) {
					bean.setOrderNum(item.getStock()); // 在庫数に制限
					itemNameListLessThan.add(item.getName());
				}
			}
		}

		// 在庫ゼロの商品を削除（拡張for文を使用）
		Iterator<BasketBean> iterator = basketList.iterator();
		while (iterator.hasNext()) {
			BasketBean bean = iterator.next();
			if (bean.getStock() == 0) {
				itemNameListZero.add(bean.getName());
				iterator.remove(); // 在庫ゼロの商品を削除
			}
		}

		model.addAttribute("basketBeans", basketList);
		model.addAttribute("itemNameListLessThan", itemNameListLessThan);
		model.addAttribute("itemNameListZero", itemNameListZero);
		session.setAttribute("basketBeans", basketList);

		return "client/basket/list";
	}

	/**
	 * 商品を買い物かごに追加する処理
	 *
	 * @param id 商品ID
	 * @return 買い物かご画面（リダイレクト）
	 */
	@PostMapping("/client/basket/add")
	public String addItem(@RequestParam("id") Integer id, Model model) {
		List<BasketBean> basketList = getBasketList();
		Optional<Item> optItem = itemRepository.findById(id);

		// 在庫不足・在庫切れメッセージを格納するリスト
		List<String> itemNameListLessThan = new ArrayList<>();
		List<String> itemNameListZero = new ArrayList<>();

		if (optItem.isEmpty()) {
			model.addAttribute("errorMessage", "指定された商品が見つかりません");
			return "redirect:/client/error"; // エラーページへリダイレクト
		}

		Item item = optItem.get();
		boolean isAdded = false;

		// **カート内の商品を検索**
		for (BasketBean bean : basketList) {
			if (bean.getId().equals(id)) {
				int newQuantity = bean.getOrderNum() + 1;

				// **在庫数を超える場合**
				if (newQuantity > item.getStock()) {
					itemNameListLessThan.add(item.getName()); // エラーリストに追加
				} else {
					bean.setOrderNum(newQuantity);
				}
				isAdded = true;
				break;
			}
		}

		// **カートになければ新しく追加（最後に追加）**
		if (!isAdded) {
			if (item.getStock() > 0) {
				BasketBean newItem = new BasketBean();
				newItem.setId(item.getId());
				newItem.setName(item.getName());
				newItem.setOrderNum(1);
				newItem.setStock(item.getStock());
				basketList.add(0, newItem); // 最新のアイテムをリストの先頭に追加
			}
		}

		// **在庫ゼロのチェック（カートから削除）**
		Iterator<BasketBean> iterator = basketList.iterator();
		while (iterator.hasNext()) {
			BasketBean bean = iterator.next();
			Optional<Item> opt = itemRepository.findById(bean.getId());
			if (opt.isPresent() && opt.get().getStock() == 0) {
				itemNameListZero.add(bean.getName()); // 在庫切れリストに追加
				iterator.remove(); // **カートから削除**
			}
		}

		// **デバッグ用**
//		System.out.println("在庫不足エラーリスト: " + itemNameListLessThan);
//		System.out.println("在庫切れエラーリスト: " + itemNameListZero);

		// **セッションへエラーメッセージとカート情報を設定**
		session.setAttribute("basketBeans", basketList);
		session.setAttribute("itemNameListLessThan", itemNameListLessThan);
		session.setAttribute("itemNameListZero", itemNameListZero);

		return "redirect:/client/basket/list";
	}

	/**
	 * 買い物かごの商品を削除する処理
	 *
	 * @param id 削除対象の商品ID
	 * @return 買い物かご一覧画面（リダイレクト）
	 */
	@PostMapping("/client/basket/delete")
	public String deleteItem(@RequestParam("id") Integer id) {
		List<BasketBean> basketList = getBasketList();

		basketList.removeIf(bean -> bean.getId().equals(id)); // 指定された商品を削除

		session.setAttribute("basketBeans", basketList);
		return "redirect:/client/basket/list";
	}

	/**
	 * 買い物かごを空にする処理
	 *
	 * @return 買い物かご一覧画面（リダイレクト）
	 */
	@PostMapping("/client/basket/allDelete")
	public String allDelete() {
		session.removeAttribute("basketBeans");
		return "redirect:/client/basket/list";
	}

	/**
	 * 買い物かごの商品個数を1減らす
	 *
	 * @param id 商品ID
	 * @return 買い物かご内の商品一覧（リダイレクト）
	 */
	@PostMapping("/client/basket/subtract")
	public String subtractCountItem(@RequestParam("id") Integer id, Model model) {
		List<BasketBean> basketList = getBasketList();

		Iterator<BasketBean> iterator = basketList.iterator();
		while (iterator.hasNext()) {
			BasketBean bean = iterator.next();
			if (bean.getId().equals(id)) {
				if (bean.getOrderNum() > 1) {
					bean.setOrderNum(bean.getOrderNum() - 1);
				} else {
					iterator.remove();
				}
				break;
			}
		}

		session.setAttribute("basketBeans", basketList);
		return "redirect:/client/basket/list";
	}

	/**
	 * セッションから買い物かご情報を取得（型安全な取得）
	 *
	 * @return 買い物かごリスト
	 */
	@SuppressWarnings("unchecked")
	private List<BasketBean> getBasketList() {
		List<BasketBean> basketBeans = (List<BasketBean>) session.getAttribute("basketBeans");
		return (basketBeans != null) ? basketBeans : new ArrayList<>();
	}
}
