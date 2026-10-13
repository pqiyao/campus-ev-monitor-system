import { ConfigEnv, UserConfigExport } from 'vite'
import vue from '@vitejs/plugin-vue'
import { viteMockServe } from 'vite-plugin-mock'
import { vitePluginSvg } from "@webxrd/vite-plugin-svg"
import { resolve } from 'path'

const pathResolve = (dir: string): any => {
  return resolve(__dirname, ".", dir)
}

const alias: Record<string, string> = {
  '@': pathResolve("src")
}

export default ({ command }: ConfigEnv): UserConfigExport => {
  const prodMock = true;
  return {
    // 系统固定部署在站点根目录。生产环境使用相对 base 会让 Vite
    // 把 public/img 中的背景图错误解析为 /assets/img/*，导致线上 404。
    base: '/',
    resolve: {
      alias
    },
    server: {
      port: 3002,
      host: '0.0.0.0',
      open: true,
      proxy: {
        // 代理配置，解决跨域问题
        '/api': {
          target: 'http://localhost:8083', // 后端服务器地址
          changeOrigin: true, // 启用跨域
          secure: false, // 如果是https接口，需要配置这个参数
          // 重写路径：去掉路径中的 /api
          // rewrite: (path) => path.replace(/^\/api/, '/api')
          // 如果您的前端路径和后端路径完全一致，不需要重写
          // rewrite: (path) => path
        },
        '/hardware-detection-1': {
          target: 'http://192.168.1.101:8092',
          changeOrigin: true,
          rewrite: (path) => path.replace(/^\/hardware-detection-1/, '')
        },
        '/hardware-detection-2': {
          target: 'http://192.168.1.101:8093',
          changeOrigin: true,
          rewrite: (path) => path.replace(/^\/hardware-detection-2/, '')
        },
        '/hardware-detection-3': {
          target: 'http://192.168.1.101:8094',
          changeOrigin: true,
          rewrite: (path) => path.replace(/^\/hardware-detection-3/, '')
        },
        '/hardware-video-1': { target: 'http://192.168.1.101:8081', changeOrigin: true, rewrite: (path) => path.replace(/^\/hardware-video-1/, '') },
        '/hardware-video-2': { target: 'http://192.168.1.101:8082', changeOrigin: true, rewrite: (path) => path.replace(/^\/hardware-video-2/, '') },
        '/hardware-video-3': { target: 'http://192.168.1.101:8083', changeOrigin: true, rewrite: (path) => path.replace(/^\/hardware-video-3/, '') },
        // 可以配置多个代理规则
        // '/dev-api': {
        //   target: 'https://www.fastmock.site/mock/xxx/',
        //   changeOrigin: true,
        //   rewrite: (path) => path.replace(/^\/dev-api/, '')
        // }
      },
    },
    build: {
      rollupOptions: {
        output: {
          manualChunks: {
            'echarts': ['echarts']
          }
        }
      }
    },
    plugins: [
      {
        name: 'dev-root-index-fallback',
        configureServer(server) {
          server.middlewares.use((request, _response, next) => {
            if (request.url === '/') request.url = '/index.html'
            next()
          })
        }
      },
      vue(),
      viteMockServe({
        mockPath: 'mock',
        localEnabled: command === 'serve',
        prodEnabled: command !== 'serve' && prodMock,
        watchFiles: true,
        injectCode: `
          import { setupProdMockServer } from '../mockProdServer';
          setupProdMockServer();
        `,
        logger: true,
      }),
      vitePluginSvg({
        // 必要的。必须是绝对路径组成的数组。
        iconDirs: [
          resolve(__dirname, 'src/assets/svg'),
        ],
        // 必要的。入口script
        main: resolve(__dirname, 'src/main.js'),
        symbolIdFormat: 'icon-[name]'
      }),
    ],
    css: {
      postcss: {
        plugins: [
          {
            postcssPlugin: 'internal:charset-removal',
            AtRule: {
              charset: (atRule) => {
                if (atRule.name === 'charset') {
                  atRule.remove();
                }
              }
            }
          }
        ],
      },
    }
  };
}
