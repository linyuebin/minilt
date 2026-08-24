import {
	createSSRApp
} from "vue";
import * as Pinia from "pinia";
import App from "./App.vue";
import { useUserStore } from "@/store/user";

export function createApp() {
	const app = createSSRApp(App);

	// 创建Pinia实例
	const pinia = Pinia.createPinia();
	app.use(pinia);

	// 恢复用户状态
	const userStore = useUserStore();
	userStore.restoreState();

	return {
		app,
		Pinia
	};
}
