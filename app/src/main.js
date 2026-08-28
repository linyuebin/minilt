import {
	createSSRApp
} from "vue";
import { createPinia } from "pinia";
import App from "./App.vue";
import { useUserStore } from "@/store/user";
import { useThemeStore } from "@/store/theme";

export function createApp() {
	const app = createSSRApp(App);

	// 创建Pinia实例
	const pinia = createPinia();
	app.use(pinia);

	// 恢复用户状态
	const userStore = useUserStore();
	userStore.restoreState();

	// 恢复主题设置（配色方案 + 自定义背景）
	const themeStore = useThemeStore();
	themeStore.restore();

	return {
		app,
		pinia
	};
}
