package jp.co.sss.shop.controller.client.basket;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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

	/**
	 * 商品情報リポジトリ
	 */
	@Autowired
	ItemRepository itemRepository;

	/**
	 * セッション管理
	 */
	@Autowired
	HttpSession session;

	/**
	 * セッションから買い物かごを安全に取得する
	 */
	private List<BasketBean> getBasketBeansFromSession() {
		Object sessionObject = session.getAttribute("basketBeans");
		if (sessionObject instanceof List<?>) {
			List<?> tempList = (List<?>) sessionObject;
			if (!tempList.isEmpty() && tempList.get(0) instanceof BasketBean) {
				// ここでストリームAPIを使って型安全にキャストする
				return tempList.stream()
						.filter(obj -> obj instanceof BasketBean)
						.map(obj -> (BasketBean) obj)
						.collect(Collectors.toList());
			}
		}
		return new ArrayList<>();
	}

	/**
	 * 商品を買い物かごに追加する処理
	 *
	 * @param id 商品ID
	 * @return 買い物かご画面
	 */
	@PostMapping("/client/basket/add")
	public String addItem(@RequestParam("id") Integer id, Model model) {
		try {
			// セッションからカート情報を安全に取得
			List<BasketBean> basketBeans = getBasketBeansFromSession();

			// 商品情報を取得
			Optional<Item> optItem = itemRepository.findById(id);
			if (optItem.isEmpty()) {
				model.addAttribute("errorMessage", "指定された商品が見つかりません");
				return "client/error"; // エラーページへ
			}

			Item item = optItem.get();
			boolean isAdded = false;

			// 既にカートにある場合は数量を増やす
			for (BasketBean bean : basketBeans) {
				if (bean.getId().equals(id)) {
					bean.setOrderNum(bean.getOrderNum() + 1);
					isAdded = true;
					break;
				}
			}

			// カートになければ新しく追加
			if (!isAdded) {
				BasketBean newItem = new BasketBean();
				newItem.setId(item.getId());
				newItem.setName(item.getName());
				newItem.setOrderNum(1);
				newItem.setStock(item.getStock());
				basketBeans.add(newItem);
			}

			// 更新したカートをセッションに保存
			session.setAttribute("basketBeans", basketBeans);

			return "client/basket/list"; // 買い物かご一覧へ遷移

		} catch (Exception e) {
			System.out.println("エラー発生: " + e.getMessage());
			model.addAttribute("errorMessage", "システムエラーが発生しました");
			return "client/error"; // エラーページへ
		}
	}

	/**
	 * 買い物かごの一覧を表示
	 *
	 * @param model Viewとの値受渡し
	 * @return 買い物かご内の商品一覧
	 */
	@GetMapping("/client/basket/list")
	public String basketList(Model model) {
		// セッションからカート情報を安全に取得
		List<BasketBean> basketBeans = getBasketBeansFromSession();

		// 在庫不足の商品リスト
		List<String> outOfStockItems = new ArrayList<>();

		// 在庫チェックを行い、在庫ゼロの商品を削除
		basketBeans.removeIf(bean -> {
			Optional<Item> optItem = itemRepository.findById(bean.getId());
			if (optItem.isPresent()) {
				Item item = optItem.get();
				if (item.getStock() == 0) {
					outOfStockItems.add(item.getName()); // 在庫ゼロの商品名をリストに追加
					return true; // カートから削除
				}
			}
			return false;
		});

		// 在庫切れメッセージの設定（仕様書の文言に合わせる）
		if (!outOfStockItems.isEmpty()) {
			model.addAttribute("itemNameListZero", outOfStockItems);
		}

		// 更新後のカート情報をセッションに保存
		session.setAttribute("basketBeans", basketBeans);

		// カートが空の場合
		if (basketBeans.isEmpty()) {
			model.addAttribute("message", "#{msg.basket.list.none}"); // 仕様書のメッセージに変更
		} else {
			model.addAttribute("basketBeans", basketBeans);
		}

		return "client/basket/list"; // Thymeleafのテンプレートを表示
	}

	/**
	 * 買い物かごの商品個数を1減らす
	 *
	 * @param id 商品ID
	 * @return 買い物かご内の商品一覧
	 */
	@PostMapping("/client/basket/subtract")
	public String subtractCountItem(@RequestParam("id") Integer id) {
		List<BasketBean> basketBeans = getBasketBeansFromSession();

		for (int i = 0; i < basketBeans.size(); i++) {
			if (basketBeans.get(i).getId().equals(id)) {
				// 数量が1以上なら減算、0なら削除
				if (basketBeans.get(i).getOrderNum() > 1) {
					basketBeans.get(i).setOrderNum(basketBeans.get(i).getOrderNum() - 1);
				} else {
					basketBeans.remove(i);
				}
				break;
			}
		}

		session.setAttribute("basketBeans", basketBeans);
		return "client/basket/list";
	}

	/**
	 * 買い物かごの商品を削除する処理
	 *
	 * @param id 削除対象の商品ID
	 * @return 買い物かご一覧画面
	 */
	@PostMapping("/client/basket/delete")
	public String deleteItem(@RequestParam("id") Integer id) {
		List<BasketBean> basketBeans = getBasketBeansFromSession();
		basketBeans.removeIf(bean -> bean.getId().equals(id));
		session.setAttribute("basketBeans", basketBeans);

		return "client/basket/list";
	}

	/**
	 * 買い物かごを空にする処理
	 *
	 * @return 買い物かご一覧画面
	 */
	@PostMapping("/client/basket/allDelete")
	public String allDelete() {
		session.removeAttribute("basketBeans");
		return "client/basket/list";
	}
}
