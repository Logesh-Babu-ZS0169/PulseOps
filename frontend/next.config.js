/** @type {import('next').NextConfig} */
const nextConfig = {
  basePath: '/kronos-studio',
  async redirects() {
    return [
      {
        source: '/',
        destination: '/kronos',
        permanent: true,
      },
    ];
  },
};

module.exports = nextConfig;